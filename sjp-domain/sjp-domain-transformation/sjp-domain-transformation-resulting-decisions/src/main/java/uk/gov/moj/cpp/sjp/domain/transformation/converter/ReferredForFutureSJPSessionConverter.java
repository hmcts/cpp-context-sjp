package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import javax.json.JsonObject;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

public class ReferredForFutureSJPSessionConverter implements Converter {
    public static final ReferredForFutureSJPSessionConverter INSTANCE = new ReferredForFutureSJPSessionConverter();

    private ReferredForFutureSJPSessionConverter() {
    }

    public JsonObject convert(final JsonObject offenceDecisionJsonObject,
                              final String pleaType,
                              final String verdict) {
        return buildOffenceDecision(offenceDecisionJsonObject);
    }

    private JsonObject buildOffenceDecision(final JsonObject offenceDecisionJsonObject) {

        return createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(TYPE, DecisionTypeCode.RSJP.getResultDecision())
                .add(OFFENCE_DECISION_INFORMATION, createArrayBuilder().add(
                        createObjectBuilder()
                                .add(OFFENCE_ID, offenceDecisionJsonObject.getString(ID))
                                .add(VERDICT, NO_VERDICT).build())
                ).build();
    }
}
