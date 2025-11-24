package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class CourtDetails implements Serializable {

    @Column(name = "national_court_code")
    private String nationalCourtCode;

    @Column(name = "national_court_name")
    private String nationalCourtName;

    public CourtDetails() {
    }

    public CourtDetails(final String nationalCourtCode, final String nationalCourtName) {
        this.nationalCourtCode = nationalCourtCode;
        this.nationalCourtName = nationalCourtName;
    }

    public String getNationalCourtCode() {
        return nationalCourtCode;
    }

    public void setNationalCourtCode(final String nationalCourtCode) {
        this.nationalCourtCode = nationalCourtCode;
    }

    public String getNationalCourtName() {
        return nationalCourtName;
    }

    public void setNationalCourtName(final String nationalCourtName) {
        this.nationalCourtName = nationalCourtName;
    }
}
