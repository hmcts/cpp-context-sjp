package uk.gov.moj.sjp.it.framework;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.fail;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

public class ShutteringIT {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final DataSource viewStoreDataSource = new TestJdbcDataSourceProvider().getViewStoreDataSource(CONTEXT_NAME);
    private final DataSource systemDataSource = new TestJdbcDataSourceProvider().getSystemDataSource(CONTEXT_NAME);
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

        systemCommandCaller.callShutter();

        final int numberOfCases = 2;

        for (int i = 0; i < numberOfCases; i++) {
            createCaseForPayloadBuilder(withDefaults().withId(randomUUID()));
        }

        final Optional<Integer> shutteredEvents = poller.pollUntilFound(() -> countEventsShuttered(numberOfCases));

        if (!shutteredEvents.isPresent()) {
            fail("Failed to shutter events");
        }

        assertThat(shutteredEvents.get() >= numberOfCases, is(true));

        assertThat(viewStoreQueryUtil.countEventsProcessed(numberOfCases), is(Optional.empty()));

        final List<UUID> idsFromViewStore = viewStoreQueryUtil.findIdsFromViewStore();

        assertThat(idsFromViewStore.size(), is(0));

        systemCommandCaller.callUnshutter();

        if (!poller.pollUntilFound(() -> viewStoreQueryUtil.countEventsProcessed(numberOfCases)).isPresent()) {
            fail();
        }

        final List<UUID> catchupIdsFromViewStore = viewStoreQueryUtil.findIdsFromViewStore();

        assertThat(catchupIdsFromViewStore.size(), is(numberOfCases));
    }

    private Optional<Integer> countEventsShuttered(final int expectedNumberOfEvents) {

        final String sql = "SELECT COUNT(*) FROM stored_command";
        try (final Connection connection = systemDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(sql);
             final ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {

                final int numberOfShutteredEvents = resultSet.getInt(1);

                if (numberOfShutteredEvents >= expectedNumberOfEvents) {
                    return of(numberOfShutteredEvents);
                }

                return empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to run " + sql, e);
        }

        return empty();
    }
}
