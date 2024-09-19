package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.event.DefendantNotEmployed;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class DeleteEmployerTest extends CaseAggregateBaseTest {

    private UUID userId = UUID.randomUUID();

    @Test
    public void shouldCreateEmployerDeletedEventIfDefendantHasEmployer() {
        assertThat(addEmployer(defendantId), is(1L));

        final Stream<Object> eventStream = caseAggregate.deleteEmployer(userId, defendantId);

        final EmployerDeleted employerDeleted = collectFirstEvent(eventStream, EmployerDeleted.class);

        assertThat(employerDeleted.getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldCreateDefendantNotEmployedEventIfDefendantHasNotEmployer() {
        final Stream<Object> eventStream = caseAggregate.deleteEmployer(userId, defendantId);

        final DefendantNotEmployed defendantNotEmployed = collectFirstEvent(eventStream, DefendantNotEmployed.class);

        assertThat(defendantNotEmployed.getDefendantId(), is(defendantId));
    }

    private Long addEmployer(final UUID defendantId) {
        final Employer employer = new Employer(defendantId, "Burger King", "12345", "023402340234",
                new Address("street", "suburb", "town", "county", "address5","ZY9 8 XW"));
        final Stream<Object> updateEmployerEvents = caseAggregate.updateEmployer(userId, employer);

        return updateEmployerEvents.filter(EmployerUpdated.class::isInstance).map(EmployerUpdated.class::cast).count();
    }

}
