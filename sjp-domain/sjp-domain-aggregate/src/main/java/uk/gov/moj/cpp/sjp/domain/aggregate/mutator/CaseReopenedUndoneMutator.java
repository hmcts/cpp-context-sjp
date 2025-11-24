package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUndone;

final class CaseReopenedUndoneMutator implements AggregateStateMutator<CaseReopenedUndone, CaseAggregateState> {

    static final CaseReopenedUndoneMutator INSTANCE = new CaseReopenedUndoneMutator();

    private CaseReopenedUndoneMutator() {
    }

    @Override
    public void apply(final CaseReopenedUndone event, final CaseAggregateState state) {
        state.setCaseReopened(false);
        state.setCaseReopenedDate(null);
    }
}
