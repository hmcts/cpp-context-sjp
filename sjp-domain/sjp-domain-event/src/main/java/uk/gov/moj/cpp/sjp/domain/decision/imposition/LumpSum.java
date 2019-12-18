package uk.gov.moj.cpp.sjp.domain.decision.imposition;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LumpSum implements Serializable {

    private BigDecimal amount;

    private Integer withinDays;

    private LocalDate payByDate;

    @JsonCreator
    public LumpSum(@JsonProperty("amount") final BigDecimal amount,
                   @JsonProperty("withinDays") final Integer withinDays,
                   @JsonProperty("payByDate") final LocalDate payByDate) {
        this.amount = amount;
        this.withinDays = withinDays;
        this.payByDate = payByDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getWithinDays() {
        return withinDays;
    }

    public LocalDate getPayByDate() {
        return payByDate;
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
