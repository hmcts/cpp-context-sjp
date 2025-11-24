package uk.gov.moj.cpp.sjp.domain.decision;

import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class Withdraw extends SingleOffenceDecision {

    private UUID withdrawalReasonId;

    public Withdraw(final UUID id, final OffenceDecisionInformation offenceDecisionInformation,
                    final UUID withdrawalReasonId) {
        this(id, offenceDecisionInformation, withdrawalReasonId, null);
    }

    @JsonCreator
    public Withdraw(@JsonProperty("id") final UUID id,
                    @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation,
                    @JsonProperty("withdrawalReasonId") final UUID withdrawalReasonId,
                    @JsonProperty("pressRestriction") final PressRestriction pressRestriction) {
        super(id, WITHDRAW, offenceDecisionInformation, pressRestriction);
        this.withdrawalReasonId = withdrawalReasonId;
    }

    public UUID getWithdrawalReasonId() {
        return withdrawalReasonId;
    }

    @Override
    public SessionCourt getConvictingCourt() {
        return null;
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public LocalDate getConvictionDate() { return null; }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
