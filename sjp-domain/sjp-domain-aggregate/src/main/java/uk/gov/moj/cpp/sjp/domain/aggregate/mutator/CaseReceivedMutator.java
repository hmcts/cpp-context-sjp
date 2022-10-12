package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import uk.gov.justice.json.schemas.domains.sjp.Language;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

final class CaseReceivedMutator implements AggregateStateMutator<CaseReceived, CaseAggregateState> {

    static final CaseReceivedMutator INSTANCE = new CaseReceivedMutator();

    private CaseReceivedMutator() {
    }

    @SuppressWarnings("squid:S1125")
    @Override
    public void apply(final CaseReceived event, final CaseAggregateState state) {
        state.setCaseId(event.getCaseId());
        state.setDefendantId(event.getDefendant().getId());
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
        state.setDefendantGender(event.getDefendant().getGender());
        state.setDefendantContactDetails(event.getDefendant().getContactDetails());
        state.setDefendantDateOfBirth(event.getDefendant().getDateOfBirth());
        state.setDefendantAddress(event.getDefendant().getAddress());
        state.setDefendantLegalEntityName(event.getDefendant().getLegalEntityName());
        state.setDefendantNationalInsuranceNumber(event.getDefendant().getNationalInsuranceNumber());
        final Language hearingLanguage = event.getDefendant().getHearingLanguage();
        if (hearingLanguage != null) {
            state.updateDefendantSpeakWelsh(event.getDefendant().getId(), hearingLanguage.equals(Language.W));
        }
        state.setExpectedDateReady(event.getExpectedDateReady());
        state.setCaseReceived(true);
        state.setPostingDate(event.getPostingDate());
        state.setDefendantRegion(event.getDefendant().getRegion());
        state.setDefendantDriverNumber(event.getDefendant().getDriverNumber());
        state.setPcqId(event.getDefendant().getPcqId());
        state.setCosts(event.getCosts());
        state.setManagedByAtcm(true);

        event.getDefendant().getOffences().stream()
                .filter(offence -> isTrue(offence.getPressRestrictable()))
                .forEach(offence -> state.markOffenceAsPressRestrictable(offence.getId()));
    }
}
