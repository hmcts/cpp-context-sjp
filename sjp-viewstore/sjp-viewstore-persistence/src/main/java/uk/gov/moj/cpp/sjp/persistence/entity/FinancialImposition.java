package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "financial_imposition")
public class FinancialImposition implements Serializable {

    @Id
    private UUID id;

    @JoinColumn(name = "case_decision_id")
    @OneToOne
    @MapsId
    private CaseDecision caseDecision;

    @Embedded
    private CostsAndSurcharge costsAndSurcharge;

    @Embedded
    private Payment payment;

    public FinancialImposition() {
    }

    public FinancialImposition(CostsAndSurcharge costsAndSurcharge, Payment payment) {
        this.costsAndSurcharge = costsAndSurcharge;
        this.payment = payment;
    }

    public CaseDecision getCaseDecision() {
        return caseDecision;
    }

    public void setCaseDecision(CaseDecision caseDecision) {
        this.caseDecision = caseDecision;
    }

    public CostsAndSurcharge getCostsAndSurcharge() {
        return costsAndSurcharge;
    }

    public void setCostsAndSurcharge(final CostsAndSurcharge costsAndSurcharge) {
        this.costsAndSurcharge = costsAndSurcharge;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(final Payment payment) {
        this.payment = payment;
    }
}
