package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("sjp.events.court-referral-created")
public class CourtReferralCreated {

    private final UUID caseId;
    private final LocalDate hearingDate;

    @JsonCreator
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
