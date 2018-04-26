package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event(CaseAssignmentRejected.EVENT_NAME)
public class CaseAssignmentRejected {

    public static final String EVENT_NAME = "sjp.events.case-assignment-rejected";

    private UUID sessionId;
    private CaseAssignmentRejected.RejectReason reason;

    public CaseAssignmentRejected(final UUID sessionId, final CaseAssignmentRejected.RejectReason reason) {
        this.sessionId = sessionId;
        this.reason = reason;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public RejectReason getReason() {
        return reason;
    }

    public enum RejectReason {
        SESSION_DOES_NOT_EXIST,
        SESSION_ENDED,
        SESSION_NOT_OWNED_BY_USER,
    }
}
