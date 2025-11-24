package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriodTimeUnit;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "licence_endorsement")
    private Boolean licenceEndorsement;

    @Column(name = "penalty_points_imposed")
    private Integer penaltyPointsImposed;

    @Column(name = "penalty_points_reason")
    @Enumerated(EnumType.STRING)
    private PenaltyPointsReason penaltyPointsReason;

    @Column(name = "additional_points_reason")
    private String additionalPointsReason;

    @Column(name = "disqualification")
    private Boolean disqualification;

    @Column(name = "disqualification_type")
    @Enumerated(EnumType.STRING)
    private DisqualificationType disqualificationType;

    @Column(name = "disqualification_period_value")
    private Integer disqualificationPeriodValue;

    @Column(name = "disqualification_period_unit")
    @Enumerated(EnumType.STRING)
    private DisqualificationPeriodTimeUnit disqualificationPeriodUnit;

    @Column(name = "notional_penalty_points")
    private Integer notionalPenaltyPoints;

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
                                           final BigDecimal excisePenalty,
                                           final LocalDate convictionDate,
                                           final PressRestriction pressRestriction) {

        super(offenceId, caseDecisionId, DecisionType.FINANCIAL_PENALTY, verdict, convictionDate, pressRestriction);
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.compensation = compensation;
        this.noCompensationReason = noCompensationReason;
        this.fine = fine;
        this.backDuty = backDuty;
        this.excisePenalty = excisePenalty;
    }

    @SuppressWarnings("squid:S00107")
    public FinancialPenaltyOffenceDecision(final UUID offenceId,
                                           final UUID caseDecisionId,
                                           final VerdictType verdict,
                                           final Boolean guiltyPleaTakenIntoAccount,
                                           final BigDecimal compensation,
                                           final String noCompensationReason,
                                           final BigDecimal fine,
                                           final BigDecimal backDuty,
                                           final BigDecimal excisePenalty,
                                           final LocalDate convictionDate,
                                           final Boolean licenceEndorsement,
                                           final Integer penaltyPointsImposed,
                                           final PenaltyPointsReason penaltyPointsReason,
                                           final String additionalPointsReason,
                                           final Boolean disqualification,
                                           final DisqualificationType disqualificationType,
                                           final Integer disqualificationPeriodValue,
                                           final DisqualificationPeriodTimeUnit disqualificationPeriodTimeUnit,
                                           final Integer notionalPenaltyPoints,
                                           final PressRestriction pressRestriction) {

        super(offenceId, caseDecisionId, DecisionType.FINANCIAL_PENALTY, verdict, convictionDate, pressRestriction);
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.compensation = compensation;
        this.noCompensationReason = noCompensationReason;
        this.fine = fine;
        this.backDuty = backDuty;
        this.excisePenalty = excisePenalty;
        this.licenceEndorsement = licenceEndorsement;
        this.penaltyPointsImposed = penaltyPointsImposed;
        this.penaltyPointsReason = penaltyPointsReason;
        this.additionalPointsReason = additionalPointsReason;
        this.disqualification = disqualification;
        this.disqualificationType = disqualificationType;
        this.disqualificationPeriodValue = disqualificationPeriodValue;
        this.disqualificationPeriodUnit = disqualificationPeriodTimeUnit;
        this.notionalPenaltyPoints = notionalPenaltyPoints;
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

    public Boolean getLicenceEndorsement() {
        return licenceEndorsement;
    }

    public void setLicenceEndorsement(final Boolean licenceEndorsement) {
        this.licenceEndorsement = licenceEndorsement;
    }

    public Integer getPenaltyPointsImposed() {
        return penaltyPointsImposed;
    }

    public void setPenaltyPointsImposed(final Integer penaltyPointsImposed) {
        this.penaltyPointsImposed = penaltyPointsImposed;
    }

    public PenaltyPointsReason getPenaltyPointsReason() {
        return penaltyPointsReason;
    }

    public void setPenaltyPointsReason(final PenaltyPointsReason penaltyPointsReason) {
        this.penaltyPointsReason = penaltyPointsReason;
    }

    public String getAdditionalPointsReason() {
        return additionalPointsReason;
    }

    public void setAdditionalPointsReason(final String additionalPointsReason) {
        this.additionalPointsReason = additionalPointsReason;
    }

    public Boolean getDisqualification() {
        return disqualification;
    }

    public void setDisqualification(final Boolean disqualification) {
        this.disqualification = disqualification;
    }

    public DisqualificationType getDisqualificationType() {
        return disqualificationType;
    }

    public void setDisqualificationType(final DisqualificationType disqualificationType) {
        this.disqualificationType = disqualificationType;
    }

    public Integer getDisqualificationPeriodValue() {
        return disqualificationPeriodValue;
    }

    public void setDisqualificationPeriodValue(final Integer disqualificationPeriodValue) {
        this.disqualificationPeriodValue = disqualificationPeriodValue;
    }

    public DisqualificationPeriodTimeUnit getDisqualificationPeriodUnit() {
        return disqualificationPeriodUnit;
    }

    public void setDisqualificationPeriodUnit(final DisqualificationPeriodTimeUnit disqualificationPeriodUnit) {
        this.disqualificationPeriodUnit = disqualificationPeriodUnit;
    }

    public Integer getNotionalPenaltyPoints() {
        return notionalPenaltyPoints;
    }

    public void setNotionalPenaltyPoints(final Integer notionalPenaltyPoints) {
        this.notionalPenaltyPoints = notionalPenaltyPoints;
    }
}
