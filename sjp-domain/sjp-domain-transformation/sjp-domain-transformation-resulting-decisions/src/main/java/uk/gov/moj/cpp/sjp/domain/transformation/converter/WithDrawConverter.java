package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import javax.json.JsonObject;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

public class WithDrawConverter implements Converter {

    private static final String DEFAULT_WITHDRAWAL_REQUEST_REASON_ID = "11b9087a-4681-3484-b2cf-684295353ac6"; // cross check this ??

    public static final WithDrawConverter INSTANCE = new WithDrawConverter();

    private WithDrawConverter() {
    }

    public JsonObject convert(final JsonObject offenceDecisionJsonObject,
                              final String pleaType,
                              final String verdict) {
        return buildOffenceDecision(offenceDecisionJsonObject);
    }

    private JsonObject buildOffenceDecision(final JsonObject offenceDecisionJsonObject) {

        return createObjectBuilder()
                .add(TransformationConstants.ID, randomUUID().toString())
                .add(WITHDRAWAL_REASON_ID, DEFAULT_WITHDRAWAL_REQUEST_REASON_ID)
                .add(TYPE, DecisionTypeCode.WDRNNOT.getResultDecision())
                .add(OFFENCE_DECISION_INFORMATION, createObjectBuilder()
                        .add(OFFENCE_ID, offenceDecisionJsonObject.getString(ID))
                        .add(VERDICT, NO_VERDICT)
                )
                .build();
    }
}
