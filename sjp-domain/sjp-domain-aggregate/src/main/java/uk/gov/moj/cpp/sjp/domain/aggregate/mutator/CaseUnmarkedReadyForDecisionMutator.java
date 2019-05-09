package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;

final class CaseUnmarkedReadyForDecisionMutator implements AggregateStateMutator<CaseUnmarkedReadyForDecision, CaseAggregateState> {

    static final CaseUnmarkedReadyForDecisionMutator INSTANCE = new CaseUnmarkedReadyForDecisionMutator();

    private CaseUnmarkedReadyForDecisionMutator() {
    }

    @Override
    public void apply(final CaseUnmarkedReadyForDecision event, final CaseAggregateState state) {
        state.setReadinessReason(null);
        state.setExpectedDateReady(event.getExpectedDateReady());
    }
}
