package uk.gov.moj.cpp.sjp.domain.decision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.UUID;

import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;

public class Withdraw extends SingleOffenceDecision {

    private UUID withdrawalReasonId;

    @JsonCreator
    public Withdraw(@JsonProperty("id") final UUID id,
                    @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation,
                    @JsonProperty("withdrawalReasonId") final UUID withdrawalReasonId) {
        super(id, WITHDRAW, offenceDecisionInformation);
        this.withdrawalReasonId = withdrawalReasonId;
    }

    public UUID getWithdrawalReasonId() {
        return withdrawalReasonId;
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
