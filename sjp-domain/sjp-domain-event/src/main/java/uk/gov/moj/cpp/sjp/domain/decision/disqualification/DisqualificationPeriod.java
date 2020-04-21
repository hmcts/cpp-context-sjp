package uk.gov.moj.cpp.sjp.domain.decision.disqualification;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DisqualificationPeriod implements Serializable {

    private Integer value;

    private DisqualificationPeriodTimeUnit unit;

    public DisqualificationPeriod(@JsonProperty("value") final Integer value,
                                  @JsonProperty("unit") final DisqualificationPeriodTimeUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public Integer getValue() {
        return value;
    }

    public DisqualificationPeriodTimeUnit getUnit() {
        return unit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DisqualificationPeriod)) {
            return false;
        }
        final DisqualificationPeriod that = (DisqualificationPeriod) o;
        return value.equals(that.value) &&
                unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }
}
