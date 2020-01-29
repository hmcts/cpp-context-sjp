package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.math.BigDecimal.ZERO;

import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.UUID;

class DecisionBuilder{

    private UUID id;
    private UUID offenceId;
    private VerdictType verdict = null;

    private DecisionBuilder(final UUID id) {
        this.id = id;
    }

    static DecisionBuilder decisionBuilder(UUID id){
        return new DecisionBuilder(id);
    }

    DecisionBuilder offenceId(UUID offenceId){
        this.offenceId = offenceId;
        return this;
    }

    DecisionBuilder verdict(VerdictType verdict){
        this.verdict = verdict;
        return this;
    }

    <T> T build(Class<T> type){
        OffenceDecisionInformation offenceDecisionInformation = new OffenceDecisionInformation(offenceId, verdict);
        if(type == FinancialPenalty.class)
        {
            return (T) new FinancialPenalty(id, offenceDecisionInformation, ZERO, ZERO, "", false, null, null);
        }
        if(type == Discharge.class)
        {
            return (T) new Discharge(id, offenceDecisionInformation, DischargeType.ABSOLUTE, null, ZERO, "", false, null);
        }
        if(type == Withdraw.class)
        {
            return (T) new Withdraw(id, offenceDecisionInformation, UUID.randomUUID());
        }
        if(type == Dismiss.class)
        {
            return (T) new Dismiss(id, offenceDecisionInformation);
        }
        return null;
    }
}
