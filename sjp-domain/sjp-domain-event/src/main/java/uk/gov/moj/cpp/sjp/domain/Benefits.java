package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Benefits implements Serializable {

    private static final long serialVersionUID = 1L;
    private String type;
    private Boolean claimed;
    private Boolean deductPenaltyPreference;

    public Benefits() {}

    public Benefits(final Boolean claimed, final String type) {
        this.claimed = claimed;
        this.type = type;
    }

    @JsonCreator
    public Benefits(
            @JsonProperty("claimed") final Boolean claimed,
            @JsonProperty("type") final String type,
            @JsonProperty("deductPenaltyPreference") final Boolean deductPenaltyPreference) {
        this.claimed = claimed;
        this.type = type;
        this.deductPenaltyPreference = deductPenaltyPreference;
    }

    public String getType() {
        return type;
    }

    public Boolean getClaimed() {
        return claimed;
    }

    public Boolean getDeductPenaltyPreference() {
        return deductPenaltyPreference;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Benefits benefits = (Benefits) o;
        return Objects.equals(type, benefits.type) &&
                Objects.equals(claimed, benefits.claimed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, claimed);
    }
}
