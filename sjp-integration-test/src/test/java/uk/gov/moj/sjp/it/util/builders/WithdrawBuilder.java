package uk.gov.moj.sjp.it.util.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;

import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;

import java.util.UUID;

public class WithdrawBuilder {

    private UUID id;
    private PressRestriction pressRestriction;
    private UUID withdrawalReasonId;

    public static WithdrawBuilder withDefaults() {
        final WithdrawBuilder builder = new WithdrawBuilder();
        builder.id = randomUUID();
        builder.pressRestriction = null;
        builder.withdrawalReasonId = randomUUID();
        return builder;
    }

    public WithdrawBuilder pressRestriction(final String name) {
        this.pressRestriction = new PressRestriction(name);
        return this;
    }

    public Withdraw build() {
        return new Withdraw(this.id,
                createOffenceDecisionInformation(this.id, NO_VERDICT),
                withdrawalReasonId,
                pressRestriction
        );
    }

    public WithdrawBuilder withdrawalReasonId(final UUID withdrawalReasonId) {
        this.withdrawalReasonId = withdrawalReasonId;
        return this;
    }

    public WithdrawBuilder id(final UUID offenceId) {
        this.id = offenceId;
        return this;
    }

    public WithdrawBuilder pressRestrictionRevoked() {
        this.pressRestriction = PressRestriction.revoked();
        return this;
    }
}
