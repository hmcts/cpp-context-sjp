package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class PaymentTerms implements Serializable {

    @Column(name = "reserve_terms")
    private boolean reserveTerms;

    @Embedded
    private LumpSum lumpSum;

    @Embedded
    private Installments installments;

    public PaymentTerms() {
    }

    public PaymentTerms(boolean reserveTerms, LumpSum lumpSum, Installments instalments) {
        this.reserveTerms = reserveTerms;
        this.lumpSum = lumpSum;
        this.installments = instalments;
    }

    public boolean isReserveTerms() {
        return reserveTerms;
    }

    public void setReserveTerms(final boolean reserveTerms) {
        this.reserveTerms = reserveTerms;
    }

    public LumpSum getLumpSum() {
        return lumpSum;
    }

    public void setLumpSum(final LumpSum lumpSum) {
        this.lumpSum = lumpSum;
    }

    public Installments getInstallments() {
        return installments;
    }

    public void setInstallments(final Installments installments) {
        this.installments = installments;
    }
}
