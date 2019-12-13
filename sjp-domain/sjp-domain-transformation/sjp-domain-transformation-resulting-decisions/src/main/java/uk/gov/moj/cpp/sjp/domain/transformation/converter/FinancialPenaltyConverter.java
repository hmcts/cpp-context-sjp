package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.ResultCodeHandler.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

public class FinancialPenaltyConverter implements Converter {
    public static final FinancialPenaltyConverter INSTANCE = new FinancialPenaltyConverter();

    private FinancialPenaltyConverter() {
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
                .add(TYPE, DecisionTypeCode.FO.getResultDecision())
                .add("offenceDecisionInformation",
                        buildOffenceInformation(offenceDecisionJsonObject.getString(ID), pleaType, verdict));

        if (results.containsKey(FINE)) {
            jsonObjectBuilder.add(FINE, results.get(FINE));
        }

        if (results.containsKey(COMPENSATION)) {
            jsonObjectBuilder.add(COMPENSATION, results.get(COMPENSATION));
        }

        if (results.containsKey(NO_COMPENSATION_REASON)) {
            jsonObjectBuilder.add(NO_COMPENSATION_REASON, results.getString(NO_COMPENSATION_REASON));
        }

        if (results.containsKey(GUILTY_PLEA_TAKEN_INTO_ACCOUNT)) {
            jsonObjectBuilder.add(GUILTY_PLEA_TAKEN_INTO_ACCOUNT, results.getBoolean(GUILTY_PLEA_TAKEN_INTO_ACCOUNT));
        }

        return jsonObjectBuilder.build();
    }

    private JsonObject convertAllTheResults(JsonObject offenceLevelJsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        offenceLevelJsonObject
                .getJsonArray(RESULTS)
                .getValuesAs(JsonObject.class)
                .stream()
                .forEach((eachResultObject) -> {
                    final String resultCode = eachResultObject.getString(CODE);
                    if (resultCode.equalsIgnoreCase(GPTAC)) {
                        handleResultCodeGPTAC(jsonObjectBuilder);
                    } else if (resultCode.equalsIgnoreCase(FO)) {
                        handleResultCodeFO(eachResultObject, jsonObjectBuilder);
                    } else if (resultCode.equalsIgnoreCase(FCOMP)) {
                        handleResultCodeFCOMP(eachResultObject, jsonObjectBuilder);
                    } else if (resultCode.equalsIgnoreCase(NCR)) {
                        handleResultCodeNCR(eachResultObject, jsonObjectBuilder);
                    }
                });

        return jsonObjectBuilder.build();
    }

    private JsonObject buildOffenceInformation(final String offenceId, final String pleaType, final String verdict) {
        final JsonObjectBuilder offenceInformationObjectBuilder =
                createObjectBuilder().add(OFFENCE_ID, offenceId);

        if(verdict == null) {
            offenceInformationObjectBuilder.add(VERDICT, "GUILTY".equals(pleaType) ? "FOUND_GUILTY" : "PROVED_SJP");
        } else {
            offenceInformationObjectBuilder.add(VERDICT, verdictCodeMap.get(verdict));
        }

        return offenceInformationObjectBuilder.build();
    }
}
