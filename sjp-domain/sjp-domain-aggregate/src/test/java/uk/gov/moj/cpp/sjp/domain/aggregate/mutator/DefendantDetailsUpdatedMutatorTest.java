package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.Test;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DefendantDetailsUpdatedMutatorTest {

    @Test
    public void shouldMutateDefendantDetails() {
        String defendantTitle = "Mr";

        String defendantFirstName = "firstName";
        String defendantLastName = "lastName";
        LocalDate dateOfBirth = LocalDate.now();

        Address defendantAddress = new Address("a1", "a2", "a3", "a4", "a5", "postcode");
        DefendantDetailsUpdated event = new DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder()
                .withTitle(defendantTitle)
                .withFirstName(defendantFirstName)
                .withLastName(defendantLastName)
                .withDateOfBirth(dateOfBirth)
                .withAddress(defendantAddress)
                .build();

        CaseAggregateState state = new CaseAggregateState();

        DefendantDetailsUpdatedMutator.INSTANCE.apply(event, state);

        assertThat(state.getDefendantTitle(), is(defendantTitle));
        assertThat(state.getDefendantFirstName(), is(defendantFirstName));
        assertThat(state.getDefendantLastName(), is(defendantLastName));
        assertThat(state.getDefendantDateOfBirth(), is(dateOfBirth));
        assertThat(state.getDefendantAddress(), is(defendantAddress));
    }
}
