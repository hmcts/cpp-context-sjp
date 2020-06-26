package uk.gov.moj.cpp.sjp.query.view.util.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.PressRestriction;
import uk.gov.moj.cpp.sjp.query.view.response.FinancialExpensesView;

import java.math.BigDecimal;

public class FinancialPenaltyOffenceDecisionBuilder {

    private PressRestriction pressRestriction;
    private BigDecimal backDuty;
    private BigDecimal excisePenalty;

    public FinancialPenaltyOffenceDecisionBuilder() {
    }

    public static FinancialPenaltyOffenceDecisionBuilder withDefaults() {
        return new FinancialPenaltyOffenceDecisionBuilder();
    }

    public FinancialPenaltyOffenceDecision build() {
        return new FinancialPenaltyOffenceDecision(
                randomUUID(),
                randomUUID(),
                PROVED_SJP,
                true,
                BigDecimal.TEN,
                null,
                BigDecimal.TEN,
                backDuty,
                excisePenalty,
                null,
                this.pressRestriction
        );
    }

    public FinancialPenaltyOffenceDecisionBuilder pressRestrictionApplied(final String name) {
        this.pressRestriction = new PressRestriction(name, true);
        return this;
    }

    public FinancialPenaltyOffenceDecisionBuilder pressRestrictionRevoked() {
        this.pressRestriction = PressRestriction.revoked();
        return this;
    }

    public FinancialPenaltyOffenceDecisionBuilder withBackDuty(final BigDecimal backDuty) {
        this.backDuty = backDuty;
        return this;
    }

    public FinancialPenaltyOffenceDecisionBuilder withExcisePenalty(final BigDecimal excisePenalty) {
        this.excisePenalty = excisePenalty;
        return this;
    }
}
