package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(DecisionType.DecisionName.WITHDRAW)
public class WithdrawOffenceDecision extends OffenceDecision {

    @Column(name = "withdrawal_reason_id")
    private UUID withdrawalReasonId;

    public WithdrawOffenceDecision() {
        super();
    }

    public WithdrawOffenceDecision(final UUID offenceId, final UUID caseDecisionId,
                                   final UUID withdrawalReasonId,
                                   final VerdictType verdict,
                                   final PressRestriction pressRestriction) {

        super(offenceId, caseDecisionId, DecisionType.WITHDRAW, verdict, null, pressRestriction);
        this.withdrawalReasonId = withdrawalReasonId;
    }

    public UUID getWithdrawalReasonId() {
        return withdrawalReasonId;
    }
}
