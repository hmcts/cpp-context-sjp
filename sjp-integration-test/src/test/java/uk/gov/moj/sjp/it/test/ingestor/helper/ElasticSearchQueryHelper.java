package uk.gov.moj.sjp.it.test.ingestor.helper;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;

import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchQueryHelper {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchQueryHelper.class);
    private static final Poller poller = new Poller(100, 3000L);
    private static final ElasticSearchIndexFinderUtil elasticSearch = new ElasticSearchIndexFinderUtil(new ElasticSearchClient());


    private ElasticSearchQueryHelper() {
    }

    public static final Poller getPoller() {
        return poller;
    }

    public static Optional<JsonObject> getElasticSearchResponse() {
        return getElasticSearchResponse(null, null);
    }

    public static Optional<JsonObject> getElasticSearchResponse(final String casePropertyName, final String casePropertyValue) {
        final Optional<JsonObject> outcome = poller.pollUntilFound(() -> {

            final JsonObject jsonObject = findAll();
            if (jsonObject == null) {
                fail("Failed to load data from ElasticSearch");
            }

            if (jsonObject.getInt("totalResults") == 1 && checkCaseProperty(jsonObject, casePropertyName, casePropertyValue)) {
                return of(jsonObject);
            }

            return empty();
        });

        if (!outcome.isPresent()) {
            log.info("Final ElasticSearch response is : {}", findAll());
        }
        assertTrue("No data found in crime_case_index data ", outcome.isPresent());
        return outcome;
    }

    private static JsonObject findAll() {
        try {
            return elasticSearch.findAll("crime_case_index");
        } catch (IOException e) {
            log.error("Failed to query ElasticSearch", e);
        }
        return null;
    }

    public static JsonObject getCaseFromElasticSearch() {
        return jsonFromString(getJsonArray(getElasticSearchResponse().get(), "index").get().getString(0));
    }

    public static JsonObject getCaseFromElasticSearch(final String casePropertyName, final String casePropertyValue) {
        return jsonFromString(getJsonArray(getElasticSearchResponse(casePropertyName, casePropertyValue).get(), "index").get().getString(0));
    }

    private static boolean checkCaseProperty(final JsonObject jsonObject, final String casePropertyName, final String casePropertyValue) {

        if (casePropertyName == null) {
            return true;
        }

        final JsonObject casePayload = jsonFromString(getJsonArray(jsonObject, "index").get().getString(0));
        return casePayload.containsKey(casePropertyName) && (casePayload.getString(casePropertyName).equals(casePropertyValue));
    }

}
