package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUndone;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CaseReopenedUndoneMutatorTest {

    @Test
    public void shouldUndoCaseReopening() {
        CaseAggregateState state = new CaseAggregateState();

        CaseReopenedUndoneMutator.INSTANCE.apply(new CaseReopenedUndone(UUID.randomUUID(), LocalDate.now()), state);

        assertFalse(state.isCaseReopened());
        assertNull(state.getCaseReopenedDate());
    }
}
