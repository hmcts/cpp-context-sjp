package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.List;

public class ReferCaseForCourtHearingCommand {

    private final CourtReferralView courtReferral;

    public ReferCaseForCourtHearingCommand(final SjpReferralView sjpReferral,
                                           final List<ProsecutionCaseView> prosecutionCases,
                                           final List<HearingRequestView> listHearingRequests) {

        this.courtReferral = new CourtReferralView(
                sjpReferral,
                prosecutionCases,
                listHearingRequests);
    }

    public CourtReferralView getCourtReferral() {
        return courtReferral;
    }

}
