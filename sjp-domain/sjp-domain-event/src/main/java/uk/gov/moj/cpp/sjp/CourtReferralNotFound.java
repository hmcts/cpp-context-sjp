package uk.gov.moj.cpp.sjp;


import uk.gov.justice.domain.annotation.Event;

@Event("sjp.events.court-referral-not-found")
public class CourtReferralNotFound {

    private final String caseId;

    public CourtReferralNotFound(final String caseId) {
        this.caseId = caseId;
    }

    public String getCaseId() {
        return caseId;
    }
}
