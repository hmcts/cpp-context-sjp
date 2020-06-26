package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriodTimeUnit;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@DiscriminatorValue(DecisionType.DecisionName.DISCHARGE)
public class DischargeOffenceDecision extends OffenceDecision {

    @Column(name="no_compensation_reason")
    private String noCompensationReason;

    @Column(name="guilty_plea_taken_into_account")
    private Boolean guiltyPleaTakenIntoAccount;

    @Column(name="compensation")
    private BigDecimal compensation;

    @Column(name="back_duty")
    private BigDecimal backDuty;

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

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "unit", column = @Column(name = "discharge_period_unit")),
            @AttributeOverride(name = "value", column = @Column(name = "discharge_period_value")),
    })
    private DischargePeriod dischargePeriod;

    @Column(name="discharge_type")
    @Enumerated(EnumType.STRING)
    private DischargeType dischargeType;

    @SuppressWarnings({"squid:S00107"})
    public DischargeOffenceDecision(final UUID offenceId, final UUID caseDecisionId,
                                    final VerdictType verdict,
                                    final DischargePeriod dischargePeriod,
                                    final Boolean guiltyPleaTakenIntoAccount,
                                    final BigDecimal compensation,
                                    final String noCompensationReason,
                                    final DischargeType dischargeType,
                                    final BigDecimal backDuty,
                                    final LocalDate convictionDate) {

        super(offenceId, caseDecisionId, DecisionType.DISCHARGE, verdict, convictionDate, null);
        this.dischargePeriod = dischargePeriod;
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.compensation = compensation;
        this.noCompensationReason = noCompensationReason;
        this.dischargeType = dischargeType;
        this.backDuty = backDuty;
    }

    @SuppressWarnings("squid:S00107")
    public DischargeOffenceDecision(final UUID offenceId, final UUID caseDecisionId,
                                    final VerdictType verdict,
                                    final DischargePeriod dischargePeriod,
                                    final Boolean guiltyPleaTakenIntoAccount,
                                    final BigDecimal compensation,
                                    final String noCompensationReason,
                                    final DischargeType dischargeType,
                                    final BigDecimal backDuty,
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

        super(offenceId, caseDecisionId, DecisionType.DISCHARGE, verdict, convictionDate, pressRestriction);
        this.dischargePeriod = dischargePeriod;
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.compensation = compensation;
        this.noCompensationReason = noCompensationReason;
        this.dischargeType = dischargeType;
        this.backDuty = backDuty;
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

    public DischargeOffenceDecision() {
        super();
    }

    public DischargeType getDischargeType() {
        return dischargeType;
    }

    public void setDischargeType(DischargeType dischargeType) {
        this.dischargeType = dischargeType;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public void setCompensation(BigDecimal compensation) {
        this.compensation = compensation;
    }

    public Boolean isGuiltyPleaTakenIntoAccount() {
        return guiltyPleaTakenIntoAccount;
    }

    public void setGuiltyPleaTakenIntoAccount(Boolean guiltyPleaTakenIntoAccount) {
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
    }

    public DischargePeriod getDischargePeriod() {
        return dischargePeriod;
    }

    public void setDischargePeriod(DischargePeriod dischargePeriod) {
        this.dischargePeriod = dischargePeriod;
    }

    public String getNoCompensationReason() {
        return noCompensationReason;
    }

    public void setNoCompensationReason(String noCompensationReason) {
        this.noCompensationReason = noCompensationReason;
    }

    public BigDecimal getBackDuty() {
        return backDuty;
    }

    public void setBackDuty(BigDecimal backDuty) {
        this.backDuty = backDuty;
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
