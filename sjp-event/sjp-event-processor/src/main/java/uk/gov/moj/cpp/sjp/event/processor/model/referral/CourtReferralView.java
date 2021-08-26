package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import uk.gov.moj.cpp.sjp.model.prosecution.ProsecutionCaseView;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public class CourtReferralView {

    private final SjpReferralView sjpReferral;
    private final List<ProsecutionCaseView> prosecutionCases;
    private final List<HearingRequestView> listHearingRequests;

    @JsonInclude(NON_EMPTY)
    private final List<CourtDocumentView> courtDocuments;

    public CourtReferralView(final SjpReferralView sjpReferral,
                             final List<ProsecutionCaseView> prosecutionCases,
                             final List<HearingRequestView> listHearingRequests,
                             final List<CourtDocumentView> courtDocuments) {

        this.sjpReferral = sjpReferral;
        this.prosecutionCases = prosecutionCases;
        this.listHearingRequests = listHearingRequests;
        this.courtDocuments = courtDocuments;
    }

    public SjpReferralView getSjpReferral() {
        return sjpReferral;
    }

    public List<ProsecutionCaseView> getProsecutionCases() {
        return prosecutionCases;
    }

    public List<HearingRequestView> getListHearingRequests() {
        return listHearingRequests;
    }

    public List<CourtDocumentView> getCourtDocuments() {
        return courtDocuments;
    }
}
