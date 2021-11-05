package uk.gov.moj.cpp.sjp.domain.decision.imposition;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FinancialImposition implements Serializable {

    private static final long serialVersionUID = 676510790444821641L;

    private CostsAndSurcharge costsAndSurcharge;

    private Payment payment;

    @JsonCreator
    public FinancialImposition(@JsonProperty("costsAndSurcharge") final CostsAndSurcharge costsAndSurcharge,
                               @JsonProperty("payment") final Payment payment) {
        this.costsAndSurcharge = costsAndSurcharge;
        this.payment = payment;
    }

    public CostsAndSurcharge getCostsAndSurcharge() {
        return costsAndSurcharge;
    }

    public Payment getPayment() {
        return payment;
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
