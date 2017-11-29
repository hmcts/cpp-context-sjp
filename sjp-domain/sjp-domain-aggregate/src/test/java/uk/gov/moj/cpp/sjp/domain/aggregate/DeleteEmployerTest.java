package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.DefendantNotEmployed;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class DeleteEmployerTest {

    private CaseAggregate caseAggregate;

    @Before
    public void init() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldCreateEmployerDeletedEventIfDefendantHasEmployer() {

        final UUID defendantId = createSjpCase().getDefendantId();
        addEmployer(defendantId);

        final Stream<Object> eventStream = caseAggregate.deleteEmployer(defendantId);

        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final EmployerDeleted employerDeleted = (EmployerDeleted) events.get(0);

        assertThat(employerDeleted.getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldCreateDefendantNotEmployedEventIfDefendantHasNotEmployer() {
        final UUID defendantId = createSjpCase().getDefendantId();

        final Stream<Object> eventStream = caseAggregate.deleteEmployer(defendantId);

        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final DefendantNotEmployed defendantNotEmployed = (DefendantNotEmployed) events.get(0);

        assertThat(defendantNotEmployed.getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldCreateDefendantNotFoundEventIfDefendantDoesNotExist() {
        final UUID defendantId = randomUUID();

        final Stream<Object> eventStream = caseAggregate.deleteEmployer(defendantId);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final DefendantNotFound defendantNotFound = (DefendantNotFound) events.get(0);

        assertThat(defendantNotFound.getDefendantId(), equalTo(defendantId.toString()));
        assertThat(defendantNotFound.getDescription(), equalTo("Update employer"));
    }

    private SjpCaseCreated createSjpCase() {
        final Case sjpCase = CaseBuilder.aDefaultSjpCase().build();
        final Stream<Object> caseCreatedEvents = caseAggregate.createCase(sjpCase, ZonedDateTime.now());
        return caseCreatedEvents.filter(SjpCaseCreated.class::isInstance).map(SjpCaseCreated.class::cast).findFirst().get();
    }

    private EmployerUpdated addEmployer(final UUID defendantId) {
        final Employer employer = new Employer(defendantId, "Burger King", "12345", "023402340234",
                new Address("street", "suburb", "town", "county", "ZY9 8 XW"));
        final Stream<Object> updateEmployerEvents = caseAggregate.updateEmployer(employer);
        return updateEmployerEvents.filter(EmployerUpdated.class::isInstance).map(EmployerUpdated.class::cast).findFirst().get();
    }

}
