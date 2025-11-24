package uk.gov.moj.cpp.sjp.query.view.converter;

import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.D;

import uk.gov.moj.cpp.sjp.query.view.service.CachedReferenceData;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class DismissDecisionResult extends AbstractOffenceDecisionResult {

    public DismissDecisionResult(final JsonObject offenceDecision,
                                 final CachedReferenceData referenceData,
                                 final String resultedOn) {
        super(offenceDecision, referenceData, resultedOn);
    }

    public JsonObjectBuilder toJsonObjectBuilder() {
        addResult(D);
        return createOffenceDecision();
    }
}
