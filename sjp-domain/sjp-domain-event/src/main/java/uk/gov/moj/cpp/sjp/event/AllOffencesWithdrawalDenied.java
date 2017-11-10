package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

// Old event. Replaced by case-update-rejected
@Event("structure.events.all-offences-withdrawal-denied")
public class AllOffencesWithdrawalDenied {

    private final UUID caseId;
    private final String reason;

    public AllOffencesWithdrawalDenied(final UUID caseId, final String assignmentNatureType) {
        this.caseId = caseId;
        this.reason = assignmentNatureType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getReason() {
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
        final AllOffencesWithdrawalDenied that = (AllOffencesWithdrawalDenied) o;
        return Objects.equals(getCaseId(), that.getCaseId()) &&
                Objects.equals(getReason(), that.getReason());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCaseId(), getReason());
    }
}
