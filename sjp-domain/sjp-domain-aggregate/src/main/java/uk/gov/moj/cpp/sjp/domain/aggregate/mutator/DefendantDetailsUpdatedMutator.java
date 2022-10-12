package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;

final class DefendantDetailsUpdatedMutator implements AggregateStateMutator<DefendantDetailsUpdated, CaseAggregateState> {

    static final DefendantDetailsUpdatedMutator INSTANCE = new DefendantDetailsUpdatedMutator();

    private DefendantDetailsUpdatedMutator() {
    }

    @Override
    public void apply(final DefendantDetailsUpdated event, final CaseAggregateState state) {
        ofNullable(event.getTitle()).ifPresent(state::setDefendantTitle);
        ofNullable(event.getFirstName()).ifPresent(state::setDefendantFirstName);
        ofNullable(event.getLastName()).ifPresent(state::setDefendantLastName);
        ofNullable(event.getDateOfBirth()).ifPresent(state::setDefendantDateOfBirth);
        ofNullable(event.getAddress()).ifPresent(state::setDefendantAddress);
        ofNullable(event.getNationalInsuranceNumber()).ifPresent(state::setDefendantNationalInsuranceNumber);
        ofNullable(event.getContactDetails()).ifPresent(state::setDefendantContactDetails);
        ofNullable(event.getGender()).ifPresent(state::setDefendantGender);
        ofNullable(event.getRegion()).ifPresent(state::setDefendantRegion);
        ofNullable(event.getDriverNumber()).ifPresent(state::setDefendantDriverNumber);
        ofNullable(event.getDriverLicenceDetails()).ifPresent(state::setDefendantDriverLicenceDetails);
        ofNullable(event.getPcqId()).ifPresent(state::setPcqId);
    }
}
