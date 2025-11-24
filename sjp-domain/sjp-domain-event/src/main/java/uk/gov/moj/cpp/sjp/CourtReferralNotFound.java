package uk.gov.moj.cpp.sjp;


import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("sjp.events.court-referral-not-found")
public class CourtReferralNotFound {

    private final UUID caseId;

    @JsonCreator
    public CourtReferralNotFound(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
