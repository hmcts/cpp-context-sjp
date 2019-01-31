package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.Test;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseReopened;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CaseReopenedMutatorTest {

    @Test
    public void shouldReopenCase() {
        LocalDate reopenedDate = LocalDate.now();
        CaseReopenDetails caseReopenDetails = new CaseReopenDetails(UUID.randomUUID(), reopenedDate, "", "");

        CaseAggregateState state = new CaseAggregateState();
        CaseReopenedMutator.INSTANCE.apply(new CaseReopened(caseReopenDetails), state);

        assertTrue(state.isCaseReopened());
        assertThat(state.getCaseReopenedDate(), is(reopenedDate));
    }
}
