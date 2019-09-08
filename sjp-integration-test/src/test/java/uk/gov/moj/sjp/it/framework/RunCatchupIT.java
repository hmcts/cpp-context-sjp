package uk.gov.moj.sjp.it.framework;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.framework.ContextNameProvider.CONTEXT_NAME;

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
import org.junit.Test;

public class RunCatchupIT {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final DataSource viewStoreDataSource = new TestJdbcDataSourceProvider().getViewStoreDataSource(CONTEXT_NAME);
    private final Poller poller = new Poller(10, 2000l);

    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private final ViewStoreQueryUtil viewStoreQueryUtil = new ViewStoreQueryUtil(viewStoreDataSource);
    private final SystemCommandCaller systemCommandCaller = new SystemCommandCaller(CONTEXT_NAME);

    @Before
    public void cleanDatabase() {

        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanSystemTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        viewStoreCleaner.cleanViewstoreTables();
    }

    @Test
    public void shouldRebuildThePublishedEventTable() throws Exception {

        final int numberOfCases = 10;

        for (int i = 0; i < numberOfCases; i++) {
            createCaseForPayloadBuilder(withDefaults().withId(randomUUID()));
        }

        final Optional<Integer> publishedEventCount = poller.pollUntilFound(() -> viewStoreQueryUtil.countEventsProcessed(numberOfCases));

        if (!publishedEventCount.isPresent()) {
            fail("Failed to process events");
        }

        assertThat(publishedEventCount.get() >= numberOfCases, is(true));

        final List<UUID> idsFromViewStore = viewStoreQueryUtil.findIdsFromViewStore();

        assertThat(idsFromViewStore.size(), is(numberOfCases));

        viewStoreCleaner.cleanViewstoreTables();
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);

        systemCommandCaller.callCatchup();

        if (!poller.pollUntilFound(() -> viewStoreQueryUtil.countEventsProcessed(numberOfCases)).isPresent()) {
            fail();
        }

        final List<UUID> catchupIdsFromViewStore = viewStoreQueryUtil.findIdsFromViewStore();

        assertThat(catchupIdsFromViewStore.size(), is(numberOfCases));

        for (int i = 0; i < catchupIdsFromViewStore.size(); i++) {
            assertThat(catchupIdsFromViewStore, hasItem(idsFromViewStore.get(i)));
        }
    }
}
