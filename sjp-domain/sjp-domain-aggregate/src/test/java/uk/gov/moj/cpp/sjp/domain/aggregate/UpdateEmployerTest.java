package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class UpdateEmployerTest {

    private CaseAggregate caseAggregate;

    @Before
    public void init() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldCreateEmployerUpdatedEventIfDefendantExists() {
        final Case aCase = CaseBuilder.aDefaultSjpCase().build();
        final Stream<Object> eventsStream = caseAggregate.createCase(aCase, ZonedDateTime.now());
        final SjpCaseCreated sjpCaseCreated  = (SjpCaseCreated) eventsStream.findFirst().get();

        final UUID defendantId = sjpCaseCreated.getDefendantId();
        final Employer employer = getEmployer(defendantId);


        final Stream<Object> eventStream = caseAggregate.updateEmployer(employer);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final EmployerUpdated employerUpdated = (EmployerUpdated) events.get(0);

        assertThat(employerUpdated.getDefendantId(), is(defendantId));
        assertThat(employerUpdated.getName(), is(employer.getName()));
        assertThat(employerUpdated.getEmployeeReference(), is(employer.getEmployeeReference()));
        assertThat(employerUpdated.getPhone(), is(employer.getPhone()));
        assertThat(employerUpdated.getAddress(), is(employer.getAddress()));
    }

    @Test
    public void shouldCreateDefendantNotFoundEventIfDefendantDoesNotExist() {
        final UUID defendantId = UUID.randomUUID();

        final Employer employer = getEmployer(defendantId);

        final Stream<Object> eventStream = caseAggregate.updateEmployer(employer);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final DefendantNotFound defendantNotFound = (DefendantNotFound) events.get(0);

        assertThat(defendantNotFound.getDefendantId(), equalTo(defendantId.toString()));
        assertThat(defendantNotFound.getDescription(), equalTo("Update employer"));
    }

    private Employer getEmployer(final UUID defendantId) {
        return new Employer(defendantId, "Burger King", "12345", "023402340234",
                new Address("street", "suburb", "town", "county", "ZY9 8 XW"));
    }
}
