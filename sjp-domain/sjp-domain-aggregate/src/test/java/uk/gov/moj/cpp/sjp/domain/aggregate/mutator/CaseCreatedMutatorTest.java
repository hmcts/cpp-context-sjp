package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import java.time.ZonedDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.MatcherAssert.assertThat;

public class CaseCreatedMutatorTest {

    @Test
    public void apply() {
        Case aCase = CaseBuilder.aDefaultSjpCase().build();

        SjpCaseCreated event = new SjpCaseCreated(
                aCase.getId(),
                aCase.getUrn(),
                aCase.getProsecutingAuthority(),
                aCase.getDefendant().getId(),
                0,
                aCase.getCosts(),
                aCase.getPostingDate(),
                aCase.getDefendant().getOffences(),
                ZonedDateTime.now());

        CaseAggregateState state = new CaseAggregateState();

        CaseCreatedMutator.INSTANCE.apply(event, state);

        assertThat(state.getCaseId(), is(event.getId()));
        assertThat(state.getUrn(), is(event.getUrn()));
        assertThat(state.getProsecutingAuthority(), is(event.getProsecutingAuthority()));
        assertThat(state.getOffenceIdsByDefendantId().entrySet(), iterableWithSize(aCase.getDefendant().getOffences().size()));
        assertThat(state.isCaseReceived(), is(true));
        assertThat(state.isManagedByAtcm(), is(true));
    }

}
