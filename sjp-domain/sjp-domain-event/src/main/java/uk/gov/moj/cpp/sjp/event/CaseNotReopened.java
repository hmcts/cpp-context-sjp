package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

/**
 * Event for rejecting an update of a reopened case if it hasn't been yet reopened
 */
@Event("structure.events.update-case-reopened-failed")
public class CaseNotReopened {

    private final String caseId;
    private final String description;

    public CaseNotReopened(String caseId, String description) {
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
