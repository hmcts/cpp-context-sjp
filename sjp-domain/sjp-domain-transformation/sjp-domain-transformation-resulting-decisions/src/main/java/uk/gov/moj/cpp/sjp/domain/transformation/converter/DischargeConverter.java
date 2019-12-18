package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.ResultCodeHandler.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

public class DischargeConverter implements Converter {

    public static final DischargeConverter INSTANCE = new DischargeConverter();

    private DischargeConverter() {
    }

    public JsonObject convert(final JsonObject offenceDecisionJsonObject,
                              final String pleaType,
                              final String verdict) {
        return buildOffenceDecision(offenceDecisionJsonObject, pleaType, verdict);
    }

    private JsonObject buildOffenceDecision(final JsonObject offenceDecisionJsonObject,
                                            final String pleaType,
                                            final String verdict) {
        final JsonObject results = convertAllTheResults(offenceDecisionJsonObject);

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(TYPE, DISCHARGE)
                .add(OFFENCE_DECISION_INFORMATION,
                        buildOffenceInformation(offenceDecisionJsonObject.getString(ID), pleaType, verdict))
                .add(DISCHARGE_TYPE, results.getString(DISCHARGE_TYPE));

        if (results.containsKey(COMPENSATION)) {
            jsonObjectBuilder.add(COMPENSATION, results.getJsonNumber(COMPENSATION).bigDecimalValue());
        }

        if (results.containsKey(NO_COMPENSATION_REASON)) {
            jsonObjectBuilder.add(NO_COMPENSATION_REASON, results.getString(NO_COMPENSATION_REASON));
        }

        if (results.containsKey(GUILTY_PLEA_TAKEN_INTO_ACCOUNT)) {
            jsonObjectBuilder.add(GUILTY_PLEA_TAKEN_INTO_ACCOUNT, results.getBoolean(GUILTY_PLEA_TAKEN_INTO_ACCOUNT));
        }

        if (CONDITIONAL.equalsIgnoreCase(results.getString(DISCHARGE_TYPE))) {
            jsonObjectBuilder.add(DISCHARGED_FOR, results.getJsonObject(DISCHARGED_FOR));
        }

        return jsonObjectBuilder.build();
    }

    @SuppressWarnings("squid:S1188")
    private JsonObject convertAllTheResults(final JsonObject offenceLevelJsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        offenceLevelJsonObject
                .getJsonArray(RESULTS)
                .getValuesAs(JsonObject.class)
                .forEach((eachResultObject) -> {
                    final String resultCode = eachResultObject.getString(CODE);
                    switch (resultCode) {
                        case AD:
                            handleResultCodeAD(jsonObjectBuilder);
                            break;
                        case CD:
                            handleResultCodeCD(eachResultObject, jsonObjectBuilder);
                            break;
                        case GPTAC:
                            handleResultCodeGPTAC(jsonObjectBuilder);
                            break;
                        case FCOMP:
                            handleResultCodeFCOMP(eachResultObject, jsonObjectBuilder);
                            break;
                        case NCR:
                            handleResultCodeNCR(eachResultObject, jsonObjectBuilder);
                            break;
                        default:
                    }
                });

        return jsonObjectBuilder.build();
    }

    private JsonObject buildOffenceInformation(final String offenceId,
                                               final String pleaType,
                                               final String verdict) {
        final JsonObjectBuilder offenceInformationObjectBuilder =
                createObjectBuilder().add(OFFENCE_ID, offenceId);

        if (verdict == null) {
            offenceInformationObjectBuilder.add(VERDICT, "GUILTY".equals(pleaType) ? "FOUND_GUILTY" : "PROVED_SJP");
        } else {
            offenceInformationObjectBuilder.add(VERDICT, verdictCodeMap.get(verdict));
        }

        return offenceInformationObjectBuilder.build();
    }

}
