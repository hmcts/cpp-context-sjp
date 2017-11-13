package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;

/**
 * Event for case reopened.
 */
@Event("sjp.events.case-reopened-in-libra")
public class CaseReopened {

    @JsonUnwrapped
    private final CaseReopenDetails caseReopenDetails;

    public CaseReopened(final CaseReopenDetails caseReopenDetails) {
        this.caseReopenDetails = caseReopenDetails;
    }

    public CaseReopenDetails getCaseReopenDetails() {
        return caseReopenDetails;
    }
}
