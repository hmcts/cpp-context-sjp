package uk.gov.moj.sjp.it.test.ingestor.helper;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.fail;

import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

public class ElasticSearchQueryHelper {
    private static final Poller poller = new Poller(1200, 1000L);
    private static final ElasticSearchIndexFinderUtil elasticSearch = new ElasticSearchIndexFinderUtil(new ElasticSearchClient());


    private ElasticSearchQueryHelper() {
    }

    public static Optional<JsonObject> getElasticSearchResponse() {
        return poller.pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearch.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });
    }
}
