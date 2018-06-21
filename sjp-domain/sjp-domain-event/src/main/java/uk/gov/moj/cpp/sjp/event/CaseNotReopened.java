package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

/**
 * Event for rejecting an update of a reopened case if it hasn't been yet reopened
 */
@Event(CaseNotReopened.EVENT_NAME)
public class CaseNotReopened {

    public static final String EVENT_NAME = "sjp.events.update-case-reopened-failed";

    private final UUID caseId;
    private final String description;

    public CaseNotReopened(UUID caseId, String description) {
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
