package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import static java.util.stream.Collectors.toSet;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

final class CaseCreatedMutator implements AggregateStateMutator<SjpCaseCreated, CaseAggregateState> {

    static final CaseCreatedMutator INSTANCE = new CaseCreatedMutator();

    private CaseCreatedMutator() {
    }

    @Override
    public void apply(final SjpCaseCreated event, final CaseAggregateState state) {
        state.setCaseId(event.getId());
        state.setUrn(event.getUrn());
        state.setProsecutingAuthority(event.getProsecutingAuthority());

        state.addOffenceIdsForDefendant(
                event.getDefendantId(),
                event.getOffences().stream()
                        .map(uk.gov.moj.cpp.sjp.domain.Offence::getId)
                        .collect(toSet()));

        state.setCaseReceived(true);
    }
}
