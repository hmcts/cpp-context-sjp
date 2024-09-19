package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Event for case reopened.
 */
@Event("sjp.events.case-reopened-in-libra")
public class CaseReopened {

    @JsonUnwrapped
    private CaseReopenDetails caseReopenDetails;

    public CaseReopened() {
    }

    public CaseReopened(final CaseReopenDetails caseReopenDetails) {
        this.caseReopenDetails = caseReopenDetails;
    }

    public CaseReopenDetails getCaseReopenDetails() {
        return caseReopenDetails;
    }
}
