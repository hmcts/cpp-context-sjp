package uk.gov.moj.cpp.sjp;


import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("sjp.events.court-referral-not-found")
public class CourtReferralNotFound {

    private final UUID caseId;

    public CourtReferralNotFound(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
