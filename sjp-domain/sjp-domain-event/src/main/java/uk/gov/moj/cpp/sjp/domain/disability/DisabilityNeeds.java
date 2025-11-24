package uk.gov.moj.cpp.sjp.domain.disability;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DisabilityNeeds implements Serializable {

    public static final DisabilityNeeds NO_DISABILITY_NEEDS = new DisabilityNeeds(null, false);

    @SuppressWarnings("squid:S1700")
    private final String disabilityNeeds;

    private final boolean needed;

    @JsonCreator
    public DisabilityNeeds(@JsonProperty("disabilityNeeds") final String disabilityNeeds,
                           @JsonProperty("needed") final boolean needed) {
        this.disabilityNeeds = disabilityNeeds;
        this.needed = needed;
    }

    public static DisabilityNeeds disabilityNeedsOf(final String disabilityNeeds) {
        return ofNullable(disabilityNeeds)
                .map(String::trim)
                .map(disabilityNeedsString -> {
                    if(isNotEmpty(disabilityNeedsString)) {
                        return new DisabilityNeeds(disabilityNeedsString, true);
                    }
                    return NO_DISABILITY_NEEDS;
                }).orElse(NO_DISABILITY_NEEDS);
    }

    public String getDisabilityNeeds() {
        return disabilityNeeds;
    }

    public boolean isNeeded() {
        return needed;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DisabilityNeeds)) {
            return false;
        }
        final DisabilityNeeds that = (DisabilityNeeds) o;
        return needed == that.needed &&
                Objects.equals(disabilityNeeds, that.disabilityNeeds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disabilityNeeds, needed);
    }
}
