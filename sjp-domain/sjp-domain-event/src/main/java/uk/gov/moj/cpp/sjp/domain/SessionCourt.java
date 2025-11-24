package uk.gov.moj.cpp.sjp.domain;

import java.util.Objects;

public class SessionCourt {
    private final String courtHouseName;
    private final String localJusticeAreaNationalCourtCode;

    public SessionCourt(final String courtHouseName, final String localJusticeAreaNationalCourtCode) {
        this.courtHouseName = courtHouseName;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public String getLocalJusticeAreaNationalCourtCode() {
        return localJusticeAreaNationalCourtCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SessionCourt that = (SessionCourt) o;
        return Objects.equals(courtHouseName, that.courtHouseName) &&
                Objects.equals(localJusticeAreaNationalCourtCode, that.localJusticeAreaNationalCourtCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courtHouseName, localJusticeAreaNationalCourtCode);
    }

    @Override
    public String toString() {
        return "SessionCourt{" +
                "courtHouseName='" + courtHouseName + '\'' +
                ", localJusticeAreaNationalCourtCode='" + localJusticeAreaNationalCourtCode + '\'' +
                '}';
    }
}
