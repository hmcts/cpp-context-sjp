package uk.gov.moj.sjp.it.test.ingestor.helper;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;

import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchQueryHelper {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchQueryHelper.class);
    private static final Poller poller = new Poller(20, 200L);
    private static final ElasticSearchIndexFinderUtil elasticSearch = new ElasticSearchIndexFinderUtil(new ElasticSearchClient());


    private ElasticSearchQueryHelper() {
    }

    public static JsonObject getCaseFromElasticSearch(final String... caseIds) {
        return jsonFromString(getJsonArray(getElasticSearchResponseWithPredicate(p -> true, caseIds).get(), "index").get().getString(0));
    }

    public static JsonObject getCaseFromElasticSearchWithPredicate(Predicate<JsonObject> casePredicate, final String... caseIds) {
        return jsonFromString(getJsonArray(getElasticSearchResponseWithPredicate(casePredicate, caseIds).get(), "index").get().getString(0));
    }

    private static Optional<JsonObject> getElasticSearchResponseWithPredicate(final Predicate<JsonObject> predicate, final String... caseIds) {
        final Optional<JsonObject> outcome = poller.pollUntilFound(() -> {

            final JsonObject jsonObject = findByCaseIds(caseIds);
            if (jsonObject == null) {
                fail("Failed to load data from ElasticSearch");
            }

            if (jsonObject.getInt("totalResults") == caseIds.length && predicate.test(jsonObject)) {
                return of(jsonObject);
            }

            return empty();
        });

        if (!outcome.isPresent()) {
            log.info("Final ElasticSearch response is : {}", findByCaseIds(caseIds));
        }

        assertTrue(outcome.isPresent(), "No data found in crime_case_index data ");
        return outcome;
    }

    private static JsonObject findByCaseIds(final String... caseIds) {
        try {
            return elasticSearch.findByCaseIds("crime_case_index", caseIds);
        } catch (IOException e) {
            log.error("Failed to query ElasticSearch", e);
        }
        return null;
    }

}
