package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Event for updating reopened case.
 */
@Event("sjp.events.case-reopened-in-libra-updated")
public class CaseReopenedUpdated {

    @JsonUnwrapped
    private final CaseReopenDetails caseReopenDetails;

    public CaseReopenedUpdated(final CaseReopenDetails caseReopenDetails) {
        this.caseReopenDetails = caseReopenDetails;
    }

    public CaseReopenDetails getCaseReopenDetails() {
        return caseReopenDetails;
    }
}
