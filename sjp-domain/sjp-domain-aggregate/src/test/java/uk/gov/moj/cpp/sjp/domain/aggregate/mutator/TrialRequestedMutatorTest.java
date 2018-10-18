package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.Test;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TrialRequestedMutatorTest {

    @Test
    public void shouldMutateCaseAggregateStateTrialDetails() {
        String unavailability = "unavailability";
        String witnessDetails = "witnessDetails";
        String witnessDispute = "witnessDispute";

        TrialRequested event = new TrialRequested(UUID.randomUUID(), unavailability, witnessDetails, witnessDispute, ZonedDateTime.now());

        CaseAggregateState state = new CaseAggregateState();

        TrialRequestedMutator.INSTANCE.apply(event, state);

        assertThat(state.isTrialRequested(), is(true));
        assertThat(state.isTrialRequestedPreviously(), is(true));
        assertThat(state.getTrialRequestedUnavailability(), is(unavailability));
        assertThat(state.getTrialRequestedWitnessDetails(), is(witnessDetails));
        assertThat(state.getTrialRequestedWitnessDispute(), is(witnessDispute));
    }
}
