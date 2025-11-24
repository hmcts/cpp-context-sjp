package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.ReasonForDeductingFromBenefits;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class Payment implements Serializable {

    @Column(name = "total_sum")
    private BigDecimal totalSum;

    @Embedded
    private CourtDetails fineTransferredTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;

    @Column(name = "reason_why_not_attached_or_deducted")
    private String reasonWhyNotAttachedOrDeducted;

    @Column(name = "reason_for_deducting_from_benefits")
    @Enumerated(EnumType.STRING)
    private ReasonForDeductingFromBenefits reasonForDeductingFromBenefits;

    @Embedded
    private PaymentTerms paymentTerms;

    public Payment() {
    }

    public Payment(BigDecimal totalSum, PaymentType paymentType, String reasonWhyNotAttachedOrDeducted,
                   ReasonForDeductingFromBenefits reasonForDeductingFromBenefits, PaymentTerms paymentTerms,
                   CourtDetails fineTransferredTo) {
        this.totalSum = totalSum;
        this.paymentType = paymentType;
        this.reasonWhyNotAttachedOrDeducted = reasonWhyNotAttachedOrDeducted;
        this.reasonForDeductingFromBenefits = reasonForDeductingFromBenefits;
        this.paymentTerms = paymentTerms;
        this.fineTransferredTo = fineTransferredTo;
    }

    public BigDecimal getTotalSum() {
        return totalSum;
    }

    public void setTotalSum(final BigDecimal totalSum) {
        this.totalSum = totalSum;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(final PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public String getReasonWhyNotAttachedOrDeducted() {
        return reasonWhyNotAttachedOrDeducted;
    }

    public void setReasonWhyNotAttachedOrDeducted(final String reasonWhyNotAttachedOrDeducted) {
        this.reasonWhyNotAttachedOrDeducted = reasonWhyNotAttachedOrDeducted;
    }

    public PaymentTerms getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(final PaymentTerms paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public ReasonForDeductingFromBenefits getReasonForDeductingFromBenefits() {
        return reasonForDeductingFromBenefits;
    }

    public void setReasonForDeductingFromBenefits(ReasonForDeductingFromBenefits reasonForDeductingFromBenefits) {
        this.reasonForDeductingFromBenefits = reasonForDeductingFromBenefits;
    }

    public CourtDetails getFineTransferredTo() {
        return fineTransferredTo;
    }

    public void setFineTransferredTo(final CourtDetails fineTransferredTo) {
        this.fineTransferredTo = fineTransferredTo;
    }
}
