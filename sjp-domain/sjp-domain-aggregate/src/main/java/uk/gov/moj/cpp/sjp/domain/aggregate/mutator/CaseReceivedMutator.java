package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import static java.util.stream.Collectors.toSet;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

final class CaseReceivedMutator implements AggregateStateMutator<CaseReceived, CaseAggregateState> {

    static final CaseReceivedMutator INSTANCE = new CaseReceivedMutator();

    private CaseReceivedMutator() {
    }

    @Override
    public void apply(final CaseReceived event, final CaseAggregateState state) {
        state.setCaseId(event.getCaseId());
        state.setUrn(event.getUrn());
        state.setProsecutingAuthority(event.getProsecutingAuthority());

        state.addOffenceIdsForDefendant(
                event.getDefendant().getId(),
                event.getDefendant().getOffences().stream()
                        .map(uk.gov.moj.cpp.sjp.domain.Offence::getId)
                        .collect(toSet()));

        state.setDefendantTitle(event.getDefendant().getTitle());
        state.setDefendantFirstName(event.getDefendant().getFirstName());
        state.setDefendantLastName(event.getDefendant().getLastName());
        state.setDefendantDateOfBirth(event.getDefendant().getDateOfBirth());
        state.setDefendantAddress(event.getDefendant().getAddress());
        state.setPostingDate(event.getPostingDate());
        state.setCaseReceived(true);
    }
}
