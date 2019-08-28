package uk.gov.moj.sjp.it.test.ingestor.helper;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;

import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

public class ElasticSearchQueryHelper {
    private static final Poller poller = new Poller(10, 2000L);
    private static final ElasticSearchIndexFinderUtil elasticSearch = new ElasticSearchIndexFinderUtil(new ElasticSearchClient());


    private ElasticSearchQueryHelper() {
    }

    public static final Poller getPoller() {
        return poller;
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

    public static JsonObject getCaseFromElasticSearch() {
        return jsonFromString(getJsonArray(getElasticSearchResponse().get(), "index").get().getString(0));
    }
}
