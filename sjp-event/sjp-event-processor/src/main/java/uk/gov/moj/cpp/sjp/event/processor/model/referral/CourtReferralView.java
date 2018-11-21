package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import java.util.List;

public class CourtReferralView {

    private final SjpReferralView sjpReferral;
    private final List<ProsecutionCaseView> prosecutionCases;
    private final List<HearingRequestView> listHearingRequests;

    public CourtReferralView(final SjpReferralView sjpReferral,
                             final List<ProsecutionCaseView> prosecutionCases,
                             final List<HearingRequestView> listHearingRequests) {

        this.sjpReferral = sjpReferral;
        this.prosecutionCases = prosecutionCases;
        this.listHearingRequests = listHearingRequests;
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


}
