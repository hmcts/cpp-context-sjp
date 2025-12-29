package uk.gov.moj.sjp.it.test.ingestor.helper;

import uk.gov.justice.services.messaging.JsonObjects;

import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;

import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;

import java.util.function.Predicate;

import javax.json.JsonObject;
import javax.json.JsonString;

public class CasePredicate {
    private CasePredicate() {
    }

    public static Predicate<JsonObject> casePropertyPredicate(final String casePropertyName, final String casePropertyValue) {
        return jsonObject -> {
            final JsonObject index = jsonFromString(getJsonArray(jsonObject, "index").get().getString(0));
            return index.containsKey(casePropertyName) && (index.getString(casePropertyName).equals(casePropertyValue));
        };
    }

    public static Predicate<JsonObject> caseStatusIs(final CaseStatus caseStatus) {
        return casePropertyPredicate("caseStatus", caseStatus.name());
    }

    public static Predicate<JsonObject> casePayloadContains(final String subString) {
        return jsonObjecy -> ((JsonString) jsonObjecy.getJsonArray("index").get(0)).getString().contains(subString);
    }
}
