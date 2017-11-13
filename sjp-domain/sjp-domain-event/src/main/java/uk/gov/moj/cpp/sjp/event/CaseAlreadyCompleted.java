package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

@Event("sjp.events.case-completion-failed")
public class CaseAlreadyCompleted {

    private final String caseId;
    private final String description;

    public CaseAlreadyCompleted(String caseId, String description) {
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
