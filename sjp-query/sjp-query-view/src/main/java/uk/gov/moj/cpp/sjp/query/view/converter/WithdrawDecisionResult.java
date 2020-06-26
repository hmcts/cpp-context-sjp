package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.WDRNNOT;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.WITHDRAW_REASON_ID;

import uk.gov.moj.cpp.sjp.query.view.service.CachedReferenceData;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class WithdrawDecisionResult extends AbstractOffenceDecisionResult {

    public WithdrawDecisionResult(final JsonObject offenceDecision, final CachedReferenceData referenceData, final String resultedOn) {
        super(offenceDecision, referenceData, resultedOn);
    }

    public JsonObjectBuilder toJsonObjectBuilder() {
        addResult(WDRNNOT, terminalEntries(2, getWithdrawalReason()));
        return createOffenceDecision();
    }

    private String getWithdrawalReason() {
        return getReferenceData().getWithdrawalReason(getWithdrawalReasonId());
    }

    private UUID getWithdrawalReasonId() {
        return fromString(getOffenceDecision().getString(WITHDRAW_REASON_ID));
    }
}
