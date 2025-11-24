package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CourtDetails implements Serializable {

    private final String nationalCourtCode;

    private final String nationalCourtName;

    public CourtDetails(@JsonProperty("nationalCourtCode") final String nationalCourtCode,
                        @JsonProperty("nationalCourtName") final String nationalCourtName) {
        this.nationalCourtCode = nationalCourtCode;
        this.nationalCourtName = nationalCourtName;
    }

    public String getNationalCourtCode() {
        return nationalCourtCode;
    }

    public String getNationalCourtName() {
        return nationalCourtName;
    }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }
}
