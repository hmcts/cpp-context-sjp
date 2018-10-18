package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

final class TrialRequestedMutator implements AggregateStateMutator<TrialRequested, CaseAggregateState> {

    static final TrialRequestedMutator INSTANCE = new TrialRequestedMutator();

    private TrialRequestedMutator() {
    }

    @Override
    public void apply(final TrialRequested event, final CaseAggregateState state) {
        state.setTrialRequested(true);
        state.setTrialRequestedPreviously(true);
        state.setTrialRequestedUnavailability(event.getUnavailability());
        state.setTrialRequestedWitnessDetails(event.getWitnessDetails());
        state.setTrialRequestedWitnessDispute(event.getWitnessDispute());
    }

}
