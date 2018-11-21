package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import java.util.List;

public class HearingRequestView {

    private final String jurisdictionType;
    private final Integer estimateMinutes;
    private final String prosecutorDatesToAvoid;
    private final String listingDirections;
    private final HearingTypeView hearingType;
    private final List<DefendantRequestView> listDefendantRequests;

    public HearingRequestView(final String jurisdictionType,
                              final Integer estimateMinutes,
                              final String prosecutorDatesToAvoid,
                              final String listingDirections,
                              final HearingTypeView hearingType,
                              final List<DefendantRequestView> listDefendantRequests) {

        this.jurisdictionType = jurisdictionType;
        this.estimateMinutes = estimateMinutes;
        this.prosecutorDatesToAvoid = prosecutorDatesToAvoid;
        this.listingDirections = listingDirections;
        this.hearingType = hearingType;
        this.listDefendantRequests = listDefendantRequests;
    }

    public String getJurisdictionType() {
        return jurisdictionType;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public String getProsecutorDatesToAvoid() {
        return prosecutorDatesToAvoid;
    }

    public String getListingDirections() {
        return listingDirections;
    }

    public HearingTypeView getHearingType() {
        return hearingType;
    }

    public List<DefendantRequestView> getListDefendantRequests() {
        return listDefendantRequests;
    }

}
