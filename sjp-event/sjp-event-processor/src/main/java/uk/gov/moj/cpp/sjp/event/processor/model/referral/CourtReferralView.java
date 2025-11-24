package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.Collections.unmodifiableList;

import uk.gov.justice.core.courts.NextHearing;
import uk.gov.moj.cpp.sjp.model.prosecution.ProsecutionCaseView;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public class CourtReferralView {

    private final SjpReferralView sjpReferral;
    private final List<ProsecutionCaseView> prosecutionCases;
    private List<HearingRequestView> listHearingRequests;
    private NextHearing nextHearing;

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

    public CourtReferralView(final SjpReferralView sjpReferral,
                             final List<ProsecutionCaseView> prosecutionCases,
                             final List<HearingRequestView> listHearingRequests,
                             final List<CourtDocumentView> courtDocuments,
                             final NextHearing nextHearing) {

        this.sjpReferral = sjpReferral;
        this.prosecutionCases = unmodifiableList(prosecutionCases);
        this.listHearingRequests = unmodifiableList(listHearingRequests);
        this.courtDocuments = unmodifiableList(courtDocuments);
        this.nextHearing = nextHearing;
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

    public NextHearing getNextHearing() {
        return nextHearing;
    }

    public List<CourtDocumentView> getCourtDocuments() {
        return courtDocuments;
    }
}
