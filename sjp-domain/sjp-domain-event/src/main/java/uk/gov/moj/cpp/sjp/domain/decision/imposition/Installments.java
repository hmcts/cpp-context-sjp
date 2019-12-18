package uk.gov.moj.cpp.sjp.domain.decision.imposition;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Installments implements Serializable {

    private BigDecimal amount;

    private InstallmentPeriod period;

    private LocalDate startDate;

    @JsonCreator
    public Installments(@JsonProperty("amount") final BigDecimal amount,
                        @JsonProperty("period") final InstallmentPeriod period,
                        @JsonProperty("startDate") final LocalDate startDate) {
        this.amount = amount;
        this.period = period;
        this.startDate = startDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public InstallmentPeriod getPeriod() {
        return period;
    }

    public LocalDate getStartDate() {
        return startDate;
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
