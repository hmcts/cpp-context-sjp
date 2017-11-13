package uk.gov.moj.cpp.sjp.event;


import uk.gov.justice.domain.annotation.Event;

@Event("sjp.events.case-not-found")
public class CaseNotFound {

    private final String caseId;
    private final String description;

    public CaseNotFound(String caseId, String description) {
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
