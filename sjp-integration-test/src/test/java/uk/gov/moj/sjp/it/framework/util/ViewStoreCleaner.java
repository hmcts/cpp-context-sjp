package uk.gov.moj.sjp.it.framework.util;

import static java.lang.String.format;

import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To be deleted in favor of existing @{@link uk.gov.moj.sjp.it.util.SjpDatabaseCleaner}
 */
public class ViewStoreCleaner {

    private static final String DELETE_OFFENCE = "delete from offence where defendant_id in ( select id from defendant where case_id='%s')";
    private static final String DELETE_FINANCIAL_MEANS = "delete from financial_means where defendant_id in ( select id from defendant where case_id='%s') ";
    private static final String DELETE_EMPLOYER = "delete from employer where defendant_id in ( select id from defendant where case_id='%s') ";
    private static final String DELETE_CASE_SEARCH_RESULT = "delete from case_search_result where case_id='%s' ";
    private static final String DELETE_CASE_DOCUMENT = "delete from case_document where case_id='%s'";
    private static final String DELETE_DEFENDANT = "delete from defendant where case_id='%s'";
    private static final String DELETE_ONLINE_PLEA = "delete from online_plea where case_id='%s'";
    private static final String DELETE_CASE_DETAILS = "delete from case_details where id='%s'";
    private static final String DELETE_READY_CASES = "delete from ready_cases where case_id='%s'";
    private static final String DELETE_PROCESSED_EVENT = "delete from processed_event where component='EVENT_INDEXER'";
    public static final String CONTEXT_NAME = "sjp";

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewStoreCleaner.class);

    public void cleanDataInViewStore(final UUID caseId) {
        final String uuid = caseId.toString();
        deleteTable(format(DELETE_OFFENCE,uuid));
        deleteTable(format(DELETE_FINANCIAL_MEANS,uuid));
        deleteTable(format(DELETE_EMPLOYER,uuid));
        deleteTable(format(DELETE_CASE_SEARCH_RESULT,uuid));
        deleteTable(format(DELETE_CASE_DOCUMENT,uuid));
        deleteTable(format(DELETE_DEFENDANT,uuid));
        deleteTable(format(DELETE_ONLINE_PLEA,uuid));
        deleteTable(format(DELETE_CASE_DETAILS,uuid));
        deleteTable(format(DELETE_READY_CASES,uuid));
        deleteTable(DELETE_PROCESSED_EVENT);
    }

    public static int deleteTable(final String query) {
        try (final Connection sjpDataViewStoreConnection = new TestJdbcConnectionProvider().getViewStoreConnection(CONTEXT_NAME);
             final Statement statement = sjpDataViewStoreConnection.createStatement()) {
            return statement.executeUpdate(query);

        } catch (SQLException exception) {
            exception.printStackTrace();
            LOGGER.error(format("SQLException while getting delete data ", query), exception);
        }

        return 0;
    }

}
