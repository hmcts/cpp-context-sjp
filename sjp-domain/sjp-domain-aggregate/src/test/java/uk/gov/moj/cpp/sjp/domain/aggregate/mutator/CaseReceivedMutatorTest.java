package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.DefendantBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.OffenceBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class CaseReceivedMutatorTest {

    @Test
    public void shouldUpdateDetailsOnCaseReceipt() {
        final Case aCase = CaseBuilder.aDefaultSjpCase().build();
        final CaseReceived event = createCaseReceived(aCase);
        final CaseAggregateState state = new CaseAggregateState();

        CaseReceivedMutator.INSTANCE.apply(event, state);

        assertThat(state.getCaseId(), is(aCase.getId()));
        assertThat(state.getUrn(), is(aCase.getUrn()));
        assertThat(state.getProsecutingAuthority(), is(aCase.getProsecutingAuthority()));
        assertThat(state.getOffenceIdsByDefendantId().entrySet(), iterableWithSize(1));
        assertThat(state.getDefendantTitle(), is(aCase.getDefendant().getTitle()));
        assertThat(state.getDefendantFirstName(), is(aCase.getDefendant().getFirstName()));
        assertThat(state.getDefendantLastName(), is(aCase.getDefendant().getLastName()));
        assertThat(state.getDefendantDateOfBirth(), is(aCase.getDefendant().getDateOfBirth()));
        assertThat(state.getDefendantAddress(), is(aCase.getDefendant().getAddress()));
        assertThat(state.getExpectedDateReady(), is(event.getExpectedDateReady()));
        assertThat(state.getDefendantId(), is(event.getDefendant().getId()));
        assertThat(state.isManagedByAtcm(), is(true));
        assertTrue(state.isCaseReceived());
    }

    @Test
    public void shouldMarkPressRestrictionOffences() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final Case aCase = CaseBuilder.aDefaultSjpCase().withDefendant(
                new DefendantBuilder().withOffences(asList(
                        OffenceBuilder.createPressRestrictableOffence(offenceId1, true),
                        OffenceBuilder.createPressRestrictableOffence(offenceId2, false),
                        OffenceBuilder.createPressRestrictableOffence(offenceId3, null))
                ).build())
                .build();
        final CaseReceived event = createCaseReceived(aCase);
        final CaseAggregateState state = new CaseAggregateState();

        CaseReceivedMutator.INSTANCE.apply(event, state);

        assertThat(state.isPressRestrictable(offenceId1), is(true));
        assertThat(state.isPressRestrictable(offenceId2), is(false));
        assertThat(state.isPressRestrictable(offenceId3), is(false));
    }

    private CaseReceived createCaseReceived(final Case aCase) {
        return new CaseReceived(
                aCase.getId(),
                aCase.getUrn(),
                aCase.getEnterpriseId(),
                aCase.getProsecutingAuthority(),
                aCase.getCosts(),
                aCase.getPostingDate(),
                aCase.getDefendant(),
                LocalDate.now(),
                ZonedDateTime.now());
    }
}
