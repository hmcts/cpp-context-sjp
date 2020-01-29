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

    private BigDecimal backDuty;

    private BigDecimal excisePenalty;

    public FinancialPenalty(@JsonProperty("id") final UUID id,
                            @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation,
                            @JsonProperty("fine") final BigDecimal fine,
                            @JsonProperty("compensation") final BigDecimal compensation,
                            @JsonProperty("noCompensationReason") final String noCompensationReason,
                            @JsonProperty("guiltyPleaTakenIntoAccount") final Boolean guiltyPleaTakenIntoAccount,
                            @JsonProperty("backDuty") final BigDecimal backDuty,
                            @JsonProperty("excisePenalty") final BigDecimal excisePenalty) {
        super(id, FINANCIAL_PENALTY, offenceDecisionInformation);
        this.compensation = compensation;
        this.fine = fine;
        this.noCompensationReason = noCompensationReason;
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.backDuty = backDuty;
        this.excisePenalty = excisePenalty;
    }

    public static FinancialPenalty createFinancialPenalty(final UUID id,
                                                          final OffenceDecisionInformation offenceDecisionInformation,
                                                          final BigDecimal fine,
                                                          final BigDecimal compensation,
                                                          final String noCompensationReason,
                                                          final Boolean guiltyPleaTakenIntoAccount) {
        return new FinancialPenalty(id, offenceDecisionInformation, fine, compensation, noCompensationReason, guiltyPleaTakenIntoAccount, null, null);
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

    public BigDecimal getBackDuty() {
        return backDuty;
    }

    public BigDecimal getExcisePenalty() {
        return excisePenalty;
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
