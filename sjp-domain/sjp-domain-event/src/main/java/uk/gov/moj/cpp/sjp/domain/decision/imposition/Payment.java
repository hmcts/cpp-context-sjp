package uk.gov.moj.cpp.sjp.domain.decision.imposition;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Payment implements Serializable {

    private BigDecimal totalSum;

    private CourtDetails fineTransferredTo;

    private PaymentType paymentType;

    private String reasonWhyNotAttachedOrDeducted;

    private ReasonForDeductingFromBenefits reasonForDeductingFromBenefits;

    private PaymentTerms paymentTerms;

    @JsonCreator
    public Payment(@JsonProperty("totalSum") final BigDecimal totalSum,
                   @JsonProperty("paymentType") final PaymentType paymentType,
                   @JsonProperty("reasonWhyNotAttachedOrDeducted") final String reasonWhyNotAttachedOrDeducted,
                   @JsonProperty("reasonForDeductingFromBenefits") final ReasonForDeductingFromBenefits reasonForDeductingFromBenefits,
                   @JsonProperty("paymentTerms") final PaymentTerms paymentTerms,
                   @JsonProperty("fineTransferredTo") final CourtDetails fineTransferredTo) {
        this.totalSum = totalSum;
        this.paymentType = paymentType;
        this.reasonWhyNotAttachedOrDeducted = reasonWhyNotAttachedOrDeducted;
        this.paymentTerms = paymentTerms;
        this.reasonForDeductingFromBenefits = reasonForDeductingFromBenefits;
        this.fineTransferredTo = fineTransferredTo;
    }

    public BigDecimal getTotalSum() {
        return totalSum;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public String getReasonWhyNotAttachedOrDeducted() {
        return reasonWhyNotAttachedOrDeducted;
    }

    public PaymentTerms getPaymentTerms() {
        return paymentTerms;
    }

    public ReasonForDeductingFromBenefits getReasonForDeductingFromBenefits() {
        return reasonForDeductingFromBenefits;
    }

    public CourtDetails getFineTransferredTo() {
        return fineTransferredTo;
    }

    public void setFineTransferredTo(final CourtDetails fineTransferredTo) {
        this.fineTransferredTo = fineTransferredTo;
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
