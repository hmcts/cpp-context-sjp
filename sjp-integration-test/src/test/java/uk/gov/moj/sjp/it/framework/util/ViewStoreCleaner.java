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
                "defendant",
                "case_details",
                "processed_event");
    }
}
