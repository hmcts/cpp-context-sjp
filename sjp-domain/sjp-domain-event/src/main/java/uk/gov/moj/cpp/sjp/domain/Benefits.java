package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.util.Objects;

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

    public Benefits(final Boolean claimed, final String type, final Boolean deductPenaltyPreference) {
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
