package uk.gov.moj.sjp.it.framework.util;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

public class ViewStoreCleaner {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    public void cleanViewstoreTables() {
        databaseCleaner.cleanViewStoreTables("sjp",
                "online_plea",
                "offence",
                "financial_means",
                "employer",
                "case_search_result",
                "offence",
                "case_document",
                "defendant",
                "online_plea",
                "session",
                "case_details",
                "ready_cases",
                "processed_event");
    }
}
