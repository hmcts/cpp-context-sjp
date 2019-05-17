package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;

final class CaseMarkedReadyForDecisionMutator implements AggregateStateMutator<CaseMarkedReadyForDecision, CaseAggregateState> {

    static final CaseMarkedReadyForDecisionMutator INSTANCE = new CaseMarkedReadyForDecisionMutator();

    private CaseMarkedReadyForDecisionMutator() {
    }

    @Override
    public void apply(final CaseMarkedReadyForDecision event, final CaseAggregateState state) {
        state.setReadinessReason(event.getReason());
        state.setExpectedDateReady(null);
    }
}
