package uk.gov.moj.cpp.sjp.event.session;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.domain.annotation.Event;

import org.apache.commons.lang3.builder.ToStringBuilder;

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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
