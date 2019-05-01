package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event(CaseNoteRejected.EVENT_NAME)
public class CaseNoteRejected {

    public static final String EVENT_NAME = "sjp.events.case-note-rejected";

    private final UUID caseId;
    private final String rejectionReason;

    public CaseNoteRejected(final UUID caseId, final String rejectionReason) {
        this.caseId = caseId;
        this.rejectionReason = rejectionReason;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
