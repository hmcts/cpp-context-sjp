package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseReopened;

final class CaseReopenedMutator implements AggregateStateMutator<CaseReopened, CaseAggregateState> {

    static final CaseReopenedMutator INSTANCE = new CaseReopenedMutator();

    private CaseReopenedMutator() {
    }

    @Override
    public void apply(final CaseReopened event, final CaseAggregateState state) {
        state.setCaseReopened(true);
        state.setCaseReopenedDate(event.getCaseReopenDetails().getReopenedDate());
    }
}
