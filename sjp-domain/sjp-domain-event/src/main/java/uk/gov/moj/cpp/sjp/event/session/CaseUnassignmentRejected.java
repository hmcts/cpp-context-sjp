package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

@Event(CaseUnassignmentRejected.EVENT_NAME)
public class CaseUnassignmentRejected {

    public static final String EVENT_NAME = "sjp.events.case-unassignment-rejected";

    private CaseUnassignmentRejected.RejectReason reason;

    public CaseUnassignmentRejected(final CaseUnassignmentRejected.RejectReason reason) {
        this.reason = reason;
    }

    public RejectReason getReason() {
        return reason;
    }

    public enum RejectReason {
        CASE_NOT_ASSIGNED
    }
}
