package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;

import java.math.BigDecimal;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "financial_means")
public class FinancialMeans {

    @Id
    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "income_payment_frequency")
    @Enumerated(value = EnumType.STRING)
    private IncomeFrequency incomePaymentFrequency;

    @Column(name = "income_payment_amount")
    private BigDecimal incomePaymentAmount;

    @Column(name = "benefits_type")
    private String benefitsType;

    @Column(name = "benefits_claimed")
    private Boolean benefitsClaimed;

    @Column(name = "employment_status")
    private String employmentStatus;

    public FinancialMeans() {
        //required for hibernate
    }

    public FinancialMeans(final UUID defendantId,
                          final IncomeFrequency incomeFrequency,
                          final BigDecimal incomeAmount,
                          final Boolean benefitsClaimed,
                          final String benefitsType,
                          final String employmentStatus) {
        this.defendantId = defendantId;
        this.incomePaymentFrequency = incomeFrequency;
        this.incomePaymentAmount = incomeAmount;
        this.benefitsType = benefitsType;
        this.benefitsClaimed = benefitsClaimed;
        this.employmentStatus = employmentStatus;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public IncomeFrequency getIncomePaymentFrequency() {
        return incomePaymentFrequency;
    }

    public void setIncomePaymentFrequency(IncomeFrequency incomePaymentFrequency) {
        this.incomePaymentFrequency = incomePaymentFrequency;
    }

    public BigDecimal getIncomePaymentAmount() {
        return incomePaymentAmount;
    }

    public void setIncomePaymentAmount(BigDecimal incomePaymentAmount) {
        this.incomePaymentAmount = incomePaymentAmount;
    }

    public String getBenefitsType() {
        return benefitsType;
    }

    public void setBenefitsType(String benefitsType) {
        this.benefitsType = benefitsType;
    }

    public Boolean getBenefitsClaimed() {
        return benefitsClaimed;
    }

    public void setBenefitsClaimed(Boolean benefitsClaimed) {
        this.benefitsClaimed = benefitsClaimed;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

}
