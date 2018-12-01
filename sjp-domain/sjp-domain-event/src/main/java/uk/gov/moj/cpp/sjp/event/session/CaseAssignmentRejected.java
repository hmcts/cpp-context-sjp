package uk.gov.moj.cpp.sjp.event.session;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(CaseAssignmentRejected.EVENT_NAME)
public class CaseAssignmentRejected {

    public static final String EVENT_NAME = "sjp.events.case-assignment-rejected";

    private CaseAssignmentRejected.RejectReason reason;

    public CaseAssignmentRejected(final CaseAssignmentRejected.RejectReason reason) {
        this.reason = reason;
    }

    public RejectReason getReason() {
        return reason;
    }

    public enum RejectReason {
        SESSION_DOES_NOT_EXIST,
        SESSION_ENDED,
        SESSION_NOT_OWNED_BY_USER,
        CASE_ASSIGNED_TO_OTHER_USER,
        CASE_COMPLETED
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CaseAssignmentRejected)) {
            return false;
        }

        final CaseAssignmentRejected that = (CaseAssignmentRejected) o;
        return reason == that.reason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reason);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
