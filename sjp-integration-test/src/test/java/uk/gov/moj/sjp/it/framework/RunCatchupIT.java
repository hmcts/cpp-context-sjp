package uk.gov.moj.sjp.it.framework;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.jmx.api.state.ApplicationManagementState.SHUTTERED;
import static uk.gov.justice.services.jmx.api.state.ApplicationManagementState.UNSHUTTERED;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.sjp.it.framework.util.ApplicationStateUtil.getApplicationState;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.setup;

import uk.gov.justice.services.jmx.system.command.client.SystemCommandCaller;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcDataSourceProvider;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.framework.util.ViewStoreQueryUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore //Disabled due to conflict with other tests, fix arriving in future framework version
public class RunCatchupIT {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final DataSource viewStoreDataSource = new TestJdbcDataSourceProvider().getViewStoreDataSource(CONTEXT_NAME);
    private final Poller poller = new Poller(10, 2000l);

    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private final ViewStoreQueryUtil viewStoreQueryUtil = new ViewStoreQueryUtil(viewStoreDataSource);
    private final SystemCommandCaller systemCommandCaller = new SystemCommandCaller(CONTEXT_NAME);

    @Before
    public void cleanDatabase() {

        systemCommandCaller.callShutter();
        assertThat(getApplicationState(SHUTTERED), is(of(SHUTTERED)));

        systemCommandCaller.callUnshutter();
        assertThat(getApplicationState(UNSHUTTERED), is(of(UNSHUTTERED)));

        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanSystemTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        viewStoreCleaner.cleanViewstoreTables();
    }

    @Test
    public void shouldRebuildThePublishedEventTable() throws Exception {

        final int numberOfCommands = 10;
        setup();
        for (int i = 0; i < numberOfCommands; i++) {
            createCaseForPayloadBuilder(withDefaults().withId(randomUUID()));
        }

        final Optional<Integer> publishedEventCount = poller.pollUntilFound(() -> viewStoreQueryUtil.countEventsProcessed(numberOfCommands));
        assertThat(publishedEventCount.isPresent(), is(true));
        assertThat(publishedEventCount.get() >= numberOfCommands, is(true));

        final Optional<List<UUID>> idsFromViewStore = poller.pollUntilFound(() -> viewStoreQueryUtil.findIdsFromViewStore(numberOfCommands));
        assertThat(idsFromViewStore.isPresent(), is(true));
        assertThat(idsFromViewStore.get().size(), is(numberOfCommands));

        viewStoreCleaner.cleanViewstoreTables();
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);

        systemCommandCaller.callCatchup();

        assertThat(poller.pollUntilFound(() -> viewStoreQueryUtil.countEventsProcessed(numberOfCommands)).isPresent(), is(true));

        final Optional<List<UUID>> catchupIdsFromViewStore = poller.pollUntilFound(() -> viewStoreQueryUtil.findIdsFromViewStore(numberOfCommands));
        assertThat(catchupIdsFromViewStore.isPresent(), is(true));
        assertThat(catchupIdsFromViewStore.get().size(), is(numberOfCommands));

        for (int i = 0; i < catchupIdsFromViewStore.get().size(); i++) {
            assertThat(catchupIdsFromViewStore.get(), hasItem(idsFromViewStore.get().get(i)));
        }
    }
}
