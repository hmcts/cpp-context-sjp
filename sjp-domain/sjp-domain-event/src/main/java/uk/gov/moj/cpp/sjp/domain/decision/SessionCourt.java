package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;

public class SessionCourt implements Serializable {

    private final String courtHouseCode;

    private final String ljaCode;

    public SessionCourt(final String courtHouseCode, final String ljaCode) {
        this.courtHouseCode = courtHouseCode;
        this.ljaCode = ljaCode;
    }

    public String getCourtHouseCode() {
        return courtHouseCode;
    }

    public String getLjaCode() { return  ljaCode; }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }
}
