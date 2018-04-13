package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.DefendantNotEmployed;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class DeleteEmployerTest {

    private CaseAggregate caseAggregate;

    private Clock clock = new UtcClock();

    @Before
    public void init() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldCreateEmployerDeletedEventIfDefendantHasEmployer() {

        final UUID defendantId = receiveCase().getDefendant().getId();
        assertThat(addEmployer(defendantId), is(1L));

        final Stream<Object> eventStream = caseAggregate.deleteEmployer(defendantId);

        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final EmployerDeleted employerDeleted = (EmployerDeleted) events.get(0);

        assertThat(employerDeleted.getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldCreateDefendantNotEmployedEventIfDefendantHasNotEmployer() {
        final UUID defendantId = receiveCase().getDefendant().getId();

        final Stream<Object> eventStream = caseAggregate.deleteEmployer(defendantId);

        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final DefendantNotEmployed defendantNotEmployed = (DefendantNotEmployed) events.get(0);

        assertThat(defendantNotEmployed.getDefendantId(), is(defendantId));
    }

    private CaseReceived receiveCase() {
        final Case sjpCase = CaseBuilder.aDefaultSjpCase().build();
        final Stream<Object> receiveCase = caseAggregate.receiveCase(sjpCase, clock.now());
        return receiveCase.filter(CaseReceived.class::isInstance).map(CaseReceived.class::cast).findFirst().get();
    }

    private Long addEmployer(final UUID defendantId) {
        final Employer employer = new Employer(defendantId, "Burger King", "12345", "023402340234",
                new Address("street", "suburb", "town", "county", "ZY9 8 XW"));
        final Stream<Object> updateEmployerEvents = caseAggregate.updateEmployer(employer);

        return updateEmployerEvents.filter(EmployerUpdated.class::isInstance).map(EmployerUpdated.class::cast).count();
    }

}
