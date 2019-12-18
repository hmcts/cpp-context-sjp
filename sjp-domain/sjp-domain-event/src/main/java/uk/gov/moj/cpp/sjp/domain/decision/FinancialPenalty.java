package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FinancialPenalty extends SingleOffenceDecision {

    private BigDecimal fine;

    private BigDecimal compensation;

    private String noCompensationReason;

    private Boolean guiltyPleaTakenIntoAccount;

    public FinancialPenalty(@JsonProperty("id") final UUID id,
                            @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation,
                            @JsonProperty("fine") final BigDecimal fine,
                            @JsonProperty("compensation") final BigDecimal compensation,
                            @JsonProperty("noCompensationReason") final String noCompensationReason,
                            @JsonProperty("guiltyPleaTakenIntoAccount") final Boolean guiltyPleaTakenIntoAccount) {
        super(id, FINANCIAL_PENALTY, offenceDecisionInformation);
        this.compensation = compensation;
        this.fine = fine;
        this.noCompensationReason = noCompensationReason;
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
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

    public BigDecimal getFine() {
        return fine;
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
