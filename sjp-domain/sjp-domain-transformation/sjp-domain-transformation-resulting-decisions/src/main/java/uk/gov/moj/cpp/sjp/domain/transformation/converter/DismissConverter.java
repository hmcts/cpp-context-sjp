package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import javax.json.JsonObject;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

public class DismissConverter implements Converter {

    public static final DismissConverter INSTANCE = new DismissConverter();

    private DismissConverter() {
    }

    public JsonObject convert(final JsonObject offenceDecisionJsonObject,
                              final String pleaType,
                              final String verdict) {
        return buildOffenceDecision(offenceDecisionJsonObject);
    }

    private JsonObject buildOffenceDecision(final JsonObject offenceDecisionJsonObject) {

        return createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(TYPE, DecisionTypeCode.D.getResultDecision())
                .add(OFFENCE_DECISION_INFORMATION, createObjectBuilder()
                        .add(OFFENCE_ID, offenceDecisionJsonObject.getString(ID))
                        .add(VERDICT, FOUND_NOT_GUILTY)
                )
                .build();
    }

}
