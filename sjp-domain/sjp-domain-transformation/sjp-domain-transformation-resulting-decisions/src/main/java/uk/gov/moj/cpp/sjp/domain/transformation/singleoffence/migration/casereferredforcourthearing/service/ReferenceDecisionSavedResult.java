package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing.service;

import javax.json.JsonObject;

public class ReferenceDecisionSavedResult {

    private final JsonObject payload;

    public ReferenceDecisionSavedResult(JsonObject payload) {
        this.payload = payload;
    }

    public JsonObject getPayload() {
        return payload;
    }

}
