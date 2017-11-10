package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

@Event("structure.events.defendant-update-failed")
public class DefendantDetailsUpdateFailed {

    private final String caseId;
    private final String defendantId;
    private final String description;

    public DefendantDetailsUpdateFailed(String caseId, String defendantId, String description) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.description = description;
    }

    public String getDefendantId() {
        return defendantId;
    }

    public String getDescription() {
        return description;
    }

    public String getCaseId() {
        return caseId;
    }
}
