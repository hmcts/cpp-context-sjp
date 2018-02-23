package uk.gov.moj.cpp.sjp.query.controller.converter;


import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.query.controller.service.ReferenceDataService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@SuppressWarnings("WeakerAccess")
public class CaseConverter {

    @Inject
    private ReferenceDataService referenceDataService;

    /**
     * Only returns a subset of the case attributes
     *
     * @param query for the metadata
     * @return caseDetails with person info added to the relevant defendant, or JsonValue.NULL if
     * there is no match
     */
    public JsonObject addOffenceReferenceDataToOffences(final JsonObject caseDetails, final JsonEnvelope query) {
        final JsonObject defendant = buildDefendant(query, caseDetails);
        return buildCaseObject(caseDetails, defendant);
    }

    private JsonObject buildDefendant(JsonEnvelope query, JsonObject caseDetails) {
        final JsonObject defendant = caseDetails.getJsonObject("defendant");
        final JsonArray decoratedOffences = buildOffencesArray(query, defendant.getJsonArray("offences"));

        final JsonObjectBuilder defendantBuilder = JsonObjects.createObjectBuilderWithFilter(defendant, field -> !field.equals("offences"));
        defendantBuilder.add("offences", decoratedOffences);
        return defendantBuilder.build();
    }

    private JsonArray buildOffencesArray(final JsonEnvelope query, final JsonArray offences) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        offences.getValuesAs(JsonObject.class)
                .stream()
                .map(offence -> buildOffenceObject(query, offence))
                .forEach(builder::add);
        return builder.build();
    }

    private JsonObject buildOffenceObject(final JsonEnvelope query, final JsonObject offence) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("id", offence.getString("id"))
                .add("wording", offence.getString("wording"))
                .add("pendingWithdrawal", offence.getBoolean("pendingWithdrawal", false));

        final JsonObject offenceReferenceData = referenceDataService
                .getOffenceReferenceData(query, offence.getString("offenceCode"), offence.getString("startDate"));

        builder.add("title", offenceReferenceData.getString("title"));
        builder.add("legislation", offenceReferenceData.getString("legislation"));

        if (offence.containsKey("plea")) {
            builder.add("plea", offence.getString("plea"));
        }
        return builder.build();
    }

    private JsonObject buildCaseObject(JsonObject caseDetails, JsonObject defendant) {
        return createObjectBuilder()
                .add("id", caseDetails.getString("id"))
                .add("urn", caseDetails.getString("urn"))
                .add("completed", caseDetails.getBoolean("completed", false))
                .add("assigned", caseDetails.getBoolean("assigned", false))
                .add("defendant", defendant)
                .build();
    }

}
