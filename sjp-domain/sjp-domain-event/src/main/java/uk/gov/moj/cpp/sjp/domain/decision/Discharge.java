package uk.gov.moj.cpp.sjp.domain.decision;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;

public class Discharge extends SingleOffenceDecision {

    private DischargeType dischargeType;

    private DischargePeriod dischargedFor;

    private BigDecimal compensation;

    private String noCompensationReason;

    private Boolean guiltyPleaTakenIntoAccount;

    public Discharge(@JsonProperty("id") final UUID id,
                     @JsonProperty("offenceDecisionInformation") OffenceDecisionInformation offenceDecisionInformation,
                     @JsonProperty("dischargeType") final DischargeType dischargeType,
                     @JsonProperty("dischargedFor") final DischargePeriod dischargedFor,
                     @JsonProperty("compensation") final BigDecimal compensation,
                     @JsonProperty("noCompensationReason") final String noCompensationReason,
                     @JsonProperty("guiltyPleaTakenIntoAccount") final Boolean guiltyPleaTakenIntoAccount) {
        super(id, DISCHARGE, offenceDecisionInformation);
        this.dischargeType = dischargeType;
        this.dischargedFor = dischargedFor;
        this.compensation = compensation;
        this.noCompensationReason = noCompensationReason;
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
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
