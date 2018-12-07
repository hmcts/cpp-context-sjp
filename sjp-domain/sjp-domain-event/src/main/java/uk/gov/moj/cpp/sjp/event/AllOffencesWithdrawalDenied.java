package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

// Old event. Replaced by case-update-rejected
@Event("sjp.events.all-offences-withdrawal-denied")
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

}
