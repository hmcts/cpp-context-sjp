package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import uk.gov.moj.cpp.sjp.model.prosecution.ProsecutionCaseView;

import java.util.List;

public class ReferCaseForCourtHearingCommand {

    private final CourtReferralView courtReferral;

    public ReferCaseForCourtHearingCommand(final SjpReferralView sjpReferral,
                                           final List<ProsecutionCaseView> prosecutionCases,
                                           final List<HearingRequestView> listHearingRequests,
                                           final List<CourtDocumentView> courtDocuments) {

        this.courtReferral = new CourtReferralView(
                sjpReferral,
                prosecutionCases,
                listHearingRequests,
                courtDocuments);
    }

    public CourtReferralView getCourtReferral() {
        return courtReferral;
    }
}
