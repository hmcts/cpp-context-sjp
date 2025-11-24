package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

//generic event used to indicate to the front-end the update to a case couldn't be applied due to business rules
@Event(CaseUpdateRejected.EVENT_NAME)
public class CaseUpdateRejected {

    public static final String EVENT_NAME = "sjp.events.case-update-rejected";

    public enum RejectReason {
        WITHDRAWAL_PENDING,
        CASE_ASSIGNED,
        CASE_COMPLETED,
        CASE_REFERRED_FOR_COURT_HEARING,
        OFFENCE_HAS_CONVICTION,
        //below applies to online-plea only
        PLEA_ALREADY_SUBMITTED,
        PLEA_REJECTED_AS_FINAL_DECISION_TAKEN_FOR_OFFENCE
    }

    private final UUID caseId;
    private final RejectReason reason;

    private CaseUpdateRejected() {
        this(null, null);
    }

    public CaseUpdateRejected(final UUID caseId, final RejectReason rejectReason) {
        this.caseId = caseId;
        this.reason = rejectReason;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public RejectReason getReason() {
        return reason;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseUpdateRejected that = (CaseUpdateRejected) o;
        return Objects.equals(caseId, that.caseId) &&
                reason == that.reason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, reason);
    }
}
