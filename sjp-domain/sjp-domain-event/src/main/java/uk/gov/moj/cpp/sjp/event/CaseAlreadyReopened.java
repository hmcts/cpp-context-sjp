package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

/**
 * Event for rejecting reopening a case if it's already been reopened
 */
@Event("sjp.events.mark-case-reopened-failed")
public class CaseAlreadyReopened {

    private final String caseId;
    private final String description;

    public CaseAlreadyReopened(String caseId, String description) {
        this.caseId = caseId;
        this.description = description;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getDescription() {
        return description;
    }
}
