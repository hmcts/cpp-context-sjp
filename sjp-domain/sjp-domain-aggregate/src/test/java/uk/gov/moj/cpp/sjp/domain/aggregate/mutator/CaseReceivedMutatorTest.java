package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.Test;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

import java.time.ZonedDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CaseReceivedMutatorTest {

    @Test
    public void shouldUpdateDetailsOnCaseReceipt() {
        Case aCase = CaseBuilder.aDefaultSjpCase().build();

        CaseReceived event = new CaseReceived(
                aCase.getId(),
                aCase.getUrn(),
                aCase.getEnterpriseId(),
                aCase.getProsecutingAuthority(),
                aCase.getCosts(),
                aCase.getPostingDate(),
                aCase.getDefendant(),
                ZonedDateTime.now());

        CaseAggregateState state = new CaseAggregateState();

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
        assertTrue(state.isCaseReceived());
        assertThat(state.getStatus(), is(CaseStatus.NO_PLEA_RECEIVED));
    }
}
