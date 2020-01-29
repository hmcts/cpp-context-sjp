package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@DiscriminatorValue(DecisionType.DecisionName.FINANCIAL_PENALTY)
public class FinancialPenaltyOffenceDecision extends OffenceDecision {

    @Column(name="no_compensation_reason")
    private String noCompensationReason;

    @Column(name="guilty_plea_taken_into_account")
    private Boolean guiltyPleaTakenIntoAccount;

    @Column(name="compensation")
    private BigDecimal compensation;

    @Column(name="fine")
    private BigDecimal fine;

    @Column(name="back_duty")
    private BigDecimal backDuty;

    @Column(name="excise_penalty")
    private BigDecimal excisePenalty;

    public FinancialPenaltyOffenceDecision() {
        super();
    }

    public FinancialPenaltyOffenceDecision(final UUID offenceId,
                                           final UUID caseDecisionId,
                                           final VerdictType verdict,
                                           final Boolean guiltyPleaTakenIntoAccount,
                                           final BigDecimal compensation,
                                           final String noCompensationReason,
                                           final BigDecimal fine,
                                           final BigDecimal backDuty,
                                           final BigDecimal excisePenalty) {

        super(offenceId, caseDecisionId, DecisionType.FINANCIAL_PENALTY, verdict);
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.compensation = compensation;
        this.noCompensationReason = noCompensationReason;
        this.fine = fine;
        this.backDuty = backDuty;
        this.excisePenalty = excisePenalty;
    }

    public String getNoCompensationReason() {
        return noCompensationReason;
    }

    public void setNoCompensationReason(String noCompensationReason) {
        this.noCompensationReason = noCompensationReason;
    }

    public Boolean isGuiltyPleaTakenIntoAccount() {
        return guiltyPleaTakenIntoAccount;
    }

    public void setGuiltyPleaTakenIntoAccount(Boolean guiltyPleaTakenIntoAccount) {
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public void setCompensation(BigDecimal compensation) {
        this.compensation = compensation;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public void setFine(BigDecimal fine) {
        this.fine = fine;
    }

    public BigDecimal getBackDuty() {
        return backDuty;
    }

    public void setBackDuty(BigDecimal backDuty) {
        this.backDuty = backDuty;
    }

    public BigDecimal getExcisePenalty() {
        return excisePenalty;
    }

    public void setExcisePenalty(BigDecimal excisePenalty) {
        this.excisePenalty = excisePenalty;
    }
}
