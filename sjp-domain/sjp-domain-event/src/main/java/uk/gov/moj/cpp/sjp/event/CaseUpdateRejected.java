package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

//generic event used to indicate to the front-end the update to a case couldn't be applied due to business rules
@Event("sjp.events.case-update-rejected")
public class CaseUpdateRejected {

    //should match CaseUpdateHelper class
    public enum RejectReason {
        WITHDRAWAL_PENDING,
        CASE_ASSIGNED,
        CASE_COMPLETED
    }

    private UUID caseId;
    private RejectReason reason;

    private CaseUpdateRejected() {}

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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseUpdateRejected that = (CaseUpdateRejected) o;
        return Objects.equals(getCaseId(), that.getCaseId()) &&
                Objects.equals(getReason(), that.getReason());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCaseId(), getReason());
    }
}
