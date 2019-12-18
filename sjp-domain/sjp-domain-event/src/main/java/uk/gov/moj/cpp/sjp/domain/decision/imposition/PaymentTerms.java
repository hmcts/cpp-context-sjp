package uk.gov.moj.cpp.sjp.domain.decision.imposition;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentTerms implements Serializable {

    private boolean reserveTerms;

    private LumpSum lumpSum;

    private Installments installments;

    @JsonCreator
    public PaymentTerms(@JsonProperty("reserveTerms") final boolean reserveTerms,
                        @JsonProperty("lumpSum") final LumpSum lumpSum,
                        @JsonProperty("installments") final Installments installments) {
        this.reserveTerms = reserveTerms;
        this.lumpSum = lumpSum;
        this.installments = installments;
    }

    public boolean isReserveTerms() {
        return reserveTerms;
    }

    public LumpSum getLumpSum() {
        return lumpSum;
    }

    public Installments getInstallments() {
        return installments;
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
