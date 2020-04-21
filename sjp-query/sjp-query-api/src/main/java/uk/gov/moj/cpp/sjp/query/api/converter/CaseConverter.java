package uk.gov.moj.cpp.sjp.query.api.converter;


import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import uk.gov.moj.cpp.sjp.query.api.service.ReferenceOffencesDataService;

@SuppressWarnings("WeakerAccess")
public class CaseConverter {

    private static final Logger LOGGER = getLogger(CaseConverter.class);
    private static final String STATUS = "status";

    @Inject
    private ReferenceOffencesDataService referenceOffencesDataService;

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

    private JsonObject buildDefendant(final JsonEnvelope query, final JsonObject caseDetails) {
        final JsonObject defendant = caseDetails.getJsonObject("defendant");
        final JsonArray decoratedOffences = buildOffencesArray(query, defendant.getJsonArray("offences"));

        final JsonObjectBuilder defendantBuilder = JsonObjects.createObjectBuilderWithFilter(defendant, field -> !"offences".equals(field));
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

        final String offenceCode = offence.getString("offenceCode");
        final JsonObject offenceReferenceData = referenceOffencesDataService
                .getOffenceReferenceData(query, offenceCode, offence.getString("startDate"));

        final JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("id", offence.getString("id"))
                .add("wording", offence.getString("wording"))
                .add("pendingWithdrawal", offence.getBoolean("pendingWithdrawal", false))
                .add("title", offenceReferenceData.getString("title"))
                .add("legislation", offenceReferenceData.getString("legislation"));

        Optional.ofNullable(offence.getString("wordingWelsh", null))
                .ifPresent(wordingWelsh -> builder.add("wordingWelsh", wordingWelsh));

        final JsonObject document = offenceReferenceData.getJsonObject("details").getJsonObject("document");
        if (document.containsKey("welsh")) {
            final JsonObject welsh = document.getJsonObject("welsh");
            Optional.ofNullable(welsh.getString("welshoffencetitle", null))
                    .ifPresent(titleWelsh -> builder.add("titleWelsh", titleWelsh));
            Optional.ofNullable(welsh.getString("welshlegislation", null))
                    .ifPresent(legislationWelsh -> builder.add("legislationWelsh", legislationWelsh));
        }
        else {
            LOGGER.warn("No referencedata offence welsh translations for offenceCode: {}", offenceCode);
        }

        Optional.ofNullable(offence.getString("plea", null))
                .ifPresent(plea -> builder.add("plea", plea));

        return builder.build();
    }

    private static JsonObject buildCaseObject(final JsonObject caseDetails, final JsonObject defendant) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("id", caseDetails.getString("id"))
                .add("urn", caseDetails.getString("urn"))
                .add("policeFlag", caseDetails.getBoolean("policeFlag", false))
                .add("completed", caseDetails.getBoolean("completed", false))
                .add("assigned", caseDetails.getBoolean("assigned", false))
                .add("postConviction", caseDetails.getBoolean("postConviction", false))
                .add("defendant", defendant);

        Optional.ofNullable(caseDetails.getString(STATUS, null)).
                ifPresent(status -> builder.add(STATUS, status));

        return builder.build();
    }

}
