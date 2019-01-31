package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;

final class DefendantDetailsUpdatedMutator implements AggregateStateMutator<DefendantDetailsUpdated, CaseAggregateState> {

    static final DefendantDetailsUpdatedMutator INSTANCE = new DefendantDetailsUpdatedMutator();

    private DefendantDetailsUpdatedMutator() {
    }

    @Override
    public void apply(final DefendantDetailsUpdated event, final CaseAggregateState state) {
        state.setDefendantTitle(event.getTitle());
        state.setDefendantFirstName(event.getFirstName());
        state.setDefendantLastName(event.getLastName());
        state.setDefendantDateOfBirth(event.getDateOfBirth());
        state.setDefendantAddress(event.getAddress());
    }
}
