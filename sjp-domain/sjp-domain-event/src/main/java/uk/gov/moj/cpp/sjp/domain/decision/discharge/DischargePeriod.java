package uk.gov.moj.cpp.sjp.domain.decision.discharge;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DischargePeriod implements Serializable {

    private int value;

    private PeriodUnit unit;

    @JsonCreator
    public DischargePeriod(@JsonProperty("value") final int value,
                           @JsonProperty("unit") final PeriodUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public int getValue() {
        return value;
    }

    public PeriodUnit getUnit() {
        return unit;
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
