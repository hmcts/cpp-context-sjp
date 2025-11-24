package uk.gov.moj.cpp.sjp.domain.resulting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CourtDetails {

    private final String courtHouseCode;
    private final String courtHouseName;
    private final String localJusticeAreaNationalCourtCode;


    public CourtDetails(
            @JsonProperty("courtHouseCode") final String courtHouseCode,
            @JsonProperty("courtHouseName") final String courtHouseName,
            @JsonProperty("localJusticeAreaNationalCourtCode") final String localJusticeAreaNationalCourtCode) {
        this.courtHouseCode = courtHouseCode;
        this.courtHouseName = courtHouseName;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;

    }

    public String getCourtHouseCode() {
        return courtHouseCode;
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public String getLocalJusticeAreaNationalCourtCode() {
        return localJusticeAreaNationalCourtCode;
    }

}
