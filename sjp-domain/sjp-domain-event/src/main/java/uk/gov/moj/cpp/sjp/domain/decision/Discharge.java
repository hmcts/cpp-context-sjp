package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;

import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class Discharge extends DisqualifyEndorseDecision implements ConvictingDecision {

    private DischargeType dischargeType;

    private DischargePeriod dischargedFor;

    private BigDecimal compensation;

    private String noCompensationReason;

    private Boolean guiltyPleaTakenIntoAccount;

    private BigDecimal backDuty;

    private LocalDate convictionDate;

    private SessionCourt convictingCourt;

    @SuppressWarnings("squid:S00107")
    public Discharge(@JsonProperty("id") final UUID id,
                     @JsonProperty("offenceDecisionInformation") OffenceDecisionInformation offenceDecisionInformation,
                     @JsonProperty("dischargeType") final DischargeType dischargeType,
                     @JsonProperty("dischargedFor") final DischargePeriod dischargedFor,
                     @JsonProperty("compensation") final BigDecimal compensation,
                     @JsonProperty("noCompensationReason") final String noCompensationReason,
                     @JsonProperty("guiltyPleaTakenIntoAccount") final Boolean guiltyPleaTakenIntoAccount,
                     @JsonProperty("backDuty") final BigDecimal backDuty,
                     @JsonProperty("licenceEndorsed") final Boolean licenceEndorsed,
                     @JsonProperty("penaltyPointsImposed") final Integer penaltyPointsImposed,
                     @JsonProperty("penaltyPointsReason") final PenaltyPointsReason penaltyPointsReason,
                     @JsonProperty("additionalPointsReason") final String additionalPointsReason,
                     @JsonProperty("disqualification") final Boolean disqualification,
                     @JsonProperty("disqualificationType") final DisqualificationType disqualificationType,
                     @JsonProperty("disqualificationPeriod") final DisqualificationPeriod disqualificationPeriod,
                     @JsonProperty("notionalPenaltyPoints") final Integer notionalPenaltyPoints,
                     @JsonProperty("pressRestriction") final PressRestriction pressRestriction) {
        super(id, DISCHARGE, offenceDecisionInformation, licenceEndorsed, penaltyPointsImposed,
                penaltyPointsReason, additionalPointsReason, disqualification, disqualificationType,
                disqualificationPeriod, notionalPenaltyPoints, pressRestriction);
        this.dischargeType = dischargeType;
        this.dischargedFor = dischargedFor;
        this.compensation = compensation;
        this.noCompensationReason = noCompensationReason;
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.backDuty = backDuty;
    }

    public static Discharge createDischarge(final UUID id,
                                            final OffenceDecisionInformation offenceDecisionInformation,
                                            final DischargeType dischargeType,
                                            final DischargePeriod dischargedFor,
                                            final BigDecimal compensation,
                                            final String noCompensationReason,
                                            final Boolean guiltyPleaTakenIntoAccount,
                                            final BigDecimal backDuty,
                                            final PressRestriction pressRestriction) {
        return new Discharge(id, offenceDecisionInformation, dischargeType, dischargedFor, compensation, noCompensationReason, guiltyPleaTakenIntoAccount, backDuty, null,
                null, null, null, null, null, null, null, pressRestriction);
    }

    public DischargeType getDischargeType() {
        return dischargeType;
    }

    public DischargePeriod getDischargedFor() {
        return dischargedFor;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public String getNoCompensationReason() {
        return noCompensationReason;
    }

    public Boolean getGuiltyPleaTakenIntoAccount() {
        return guiltyPleaTakenIntoAccount;
    }

    public BigDecimal getBackDuty() {
        return backDuty;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public void setConvictionDate(final LocalDate convictionDate) {
        this.convictionDate = convictionDate;
    }

    public SessionCourt getConvictingCourt() {
        return convictingCourt;
    }

    public void setConvictingCourt(final SessionCourt convictingCourt) {
        this.convictingCourt = convictingCourt;
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
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
