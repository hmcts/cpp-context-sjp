package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import uk.gov.justice.services.common.converter.ZonedDateTimes;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.ResultCodeHandler.handleResultCodeSMRTO;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

import org.apache.commons.lang3.StringUtils;

public class ReferredToOpenCourtConverter implements Converter {
    public static final ReferredToOpenCourtConverter INSTANCE = new ReferredToOpenCourtConverter();

    private ReferredToOpenCourtConverter() {
    }

    public JsonObject convert(final JsonObject offenceDecisionJsonObject,
                              final String pleaType,
                              final String verdict) {
        return buildOffenceDecision(offenceDecisionJsonObject);
    }

    private JsonObject buildOffenceDecision(final JsonObject offenceDecisionJsonObject) {
        final JsonObject results = convertAllTheResults(offenceDecisionJsonObject);
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(TYPE, DecisionTypeCode.SUMRTO.getResultDecision())
                .add(OFFENCE_DECISION_INFORMATION, createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(OFFENCE_ID, offenceDecisionJsonObject.getString(ID))
                                .add(VERDICT, NO_VERDICT).build())
                );

        if (results.containsKey(REFERRED_TO_COURT)) {
            jsonObjectBuilder.add(REFERRED_TO_COURT, results.getString(REFERRED_TO_COURT));
        }

        if (results.containsKey(REFERRED_TO_ROOM)) {
            jsonObjectBuilder.add(REFERRED_TO_ROOM, results.getInt(REFERRED_TO_ROOM));
        }

        jsonObjectBuilder.add(REFERRED_TO_DATE_TIME, resolveReferredAt(results));

        if (results.containsKey(REASON)) {
            jsonObjectBuilder.add(REASON, results.getString(REASON));
        }

        if (results.containsKey(MAGISTRATES_COURT)) {
            jsonObjectBuilder.add(MAGISTRATES_COURT, results.getString(MAGISTRATES_COURT));
        }

        return jsonObjectBuilder.build();
    }

    private String resolveReferredAt(JsonObject results) {
        String timeOfHearing = "00:00";
        final String dateOfHearing = results.getString(DATE_OF_HEARING); // mandatory

        if (results.containsKey(TIME_OF_HEARING)
                && StringUtils.isNotBlank(results.getString(TIME_OF_HEARING, null))) {
            timeOfHearing = results.getString(TIME_OF_HEARING);
        }

        final String dateTimeString = dateOfHearing + "T" + timeOfHearing + "Z";
        // to be sure it parses
        final ZonedDateTime zonedDateTime = ZonedDateTimes.fromString(dateTimeString);
        return ZonedDateTimes.toString(zonedDateTime);
    }

    private JsonObject convertAllTheResults(final JsonObject offenceLevelJsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        offenceLevelJsonObject
                .getJsonArray(RESULTS)
                .getValuesAs(JsonObject.class)
                .forEach((eachResultObject) -> {
                    final String resultCode = eachResultObject.getString(CODE);
                    if (resultCode.equalsIgnoreCase(SUMRTO)) {
                        handleResultCodeSMRTO(eachResultObject, jsonObjectBuilder);
                    }
                });

        return jsonObjectBuilder.build();
    }


}
