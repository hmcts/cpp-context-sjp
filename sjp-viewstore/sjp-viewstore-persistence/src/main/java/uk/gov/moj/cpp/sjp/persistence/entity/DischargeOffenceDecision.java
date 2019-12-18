package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
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
                                    final DischargeType dischargeType) {

        super(offenceId, caseDecisionId, DecisionType.DISCHARGE,verdict);
        this.dischargePeriod = dischargePeriod;
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.compensation = compensation;
        this.noCompensationReason = noCompensationReason;
        this.dischargeType = dischargeType;
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
}
