package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

/**
 * Event for rejecting reopening a case if it's already been reopened
 */
@Event("sjp.events.mark-case-reopened-failed")
public class CaseAlreadyReopened {

    private final UUID caseId;
    private final String description;

    public CaseAlreadyReopened(UUID caseId, String description) {
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
