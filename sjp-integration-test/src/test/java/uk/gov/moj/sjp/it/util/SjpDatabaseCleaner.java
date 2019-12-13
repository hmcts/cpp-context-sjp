package uk.gov.moj.sjp.it.util;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SjpDatabaseCleaner {
    private static final String SJP_NAME = "sjp";

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    public void cleanAll() throws SQLException {
        cleanSnapshot();
        databaseCleaner.cleanEventLogTable(SJP_NAME);
        databaseCleaner.cleanStreamBufferTable(SJP_NAME);
        databaseCleaner.cleanStreamStatusTable(SJP_NAME);

        databaseCleaner.cleanViewStoreTables(
                SJP_NAME,
                "financial_means",
                "employer",
                "case_decision",
                "case_search_result",
                "offence",
                "case_document",
                "defendant",
                "offence_decision",
                "online_plea",
                "session",
                "case_details",
                "ready_cases"
        );
    }

    private void cleanSnapshot() throws SQLException {
        final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();
        try (final Connection sjpEventStoreConnection = testJdbcConnectionProvider.getEventStoreConnection("sjp");
             final Statement statement = sjpEventStoreConnection.createStatement()) {
            statement.execute("DELETE FROM snapshot");
        }
    }
}
