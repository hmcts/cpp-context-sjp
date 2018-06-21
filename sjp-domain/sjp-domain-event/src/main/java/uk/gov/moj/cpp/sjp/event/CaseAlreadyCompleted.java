package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.util.UUID;

@Event("sjp.events.case-completion-failed")
public class CaseAlreadyCompleted implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID caseId;
    private final String description;

    public CaseAlreadyCompleted(UUID caseId, String description) {
        this.caseId = caseId;
        this.description = description;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getDescription() {
        return description;
    }
}
