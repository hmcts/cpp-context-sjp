package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.util.UUID;

@Event("structure.events.court-referral-created")
public class CourtReferralCreated {

    private final UUID caseId;
    private final LocalDate hearingDate;

    public CourtReferralCreated(final UUID caseId, final LocalDate hearingDate) {
        this.caseId = caseId;
        this.hearingDate = hearingDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }
}
