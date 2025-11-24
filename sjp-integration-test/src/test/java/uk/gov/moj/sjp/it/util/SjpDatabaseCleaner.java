package uk.gov.moj.sjp.it.util;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SjpDatabaseCleaner {
    private static final String SJP_NAME = "sjp";

    private static final DatabaseCleaner DATABASE_CLEANER = new DatabaseCleaner();

    public static void cleanViewStore() throws SQLException {
        cleanSnapshot();

        DATABASE_CLEANER.cleanViewStoreTables(
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
                "case_application",
                "application_decision",
                "ready_cases",
                "case_assignment_restriction",
                "case_publish_status",
                "reserve_case"
        );
    }

    public static void cleanAll() throws SQLException {
        cleanSnapshot();

        DATABASE_CLEANER.cleanViewStoreTables(
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
                "ready_cases",
                "case_assignment_restriction"
        );
    }

    private static void cleanSnapshot() throws SQLException {
        final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();
        try (final Connection sjpEventStoreConnection = testJdbcConnectionProvider.getEventStoreConnection("sjp");
             final Statement statement = sjpEventStoreConnection.createStatement()) {
            statement.execute("DELETE FROM snapshot");
        }
    }
}
