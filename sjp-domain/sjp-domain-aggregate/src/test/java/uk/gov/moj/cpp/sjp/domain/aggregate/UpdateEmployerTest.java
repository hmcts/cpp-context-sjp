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
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
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
    public void shouldCreateEmployerUpdatedAndEmploymentStatusUpdatedEventsIfDefendantDoesNotHaveEmploymentStatus() {

        final UUID defendantId = createSjpCase().getDefendantId();
        final Employer employer = getEmployer(defendantId);

        final Stream<Object> eventStream = caseAggregate.updateEmployer(employer);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(2));

        final EmployerUpdated employerUpdated = (EmployerUpdated) events.get(0);
        final EmploymentStatusUpdated employmentStatusUpdated = (EmploymentStatusUpdated) events.get(1);

        assertEmployer(employerUpdated, employer);

        assertThat(employmentStatusUpdated.getDefendantId(), is(defendantId));
        assertThat(employmentStatusUpdated.getEmploymentStatus(), is("EMPLOYED"));
    }

    @Test
    public void shouldCreateOnlyEmployerUpdatedEventIfDefendantEmploymentStatusIsEmployed() {

        final UUID defendantId = createSjpCase().getDefendantId();
        final Employer employer = getEmployer(defendantId);
        final FinancialMeans financialMeans = getFinancialMeans(defendantId, "EMPLOYED");

        caseAggregate.updateFinancialMeans(financialMeans);

        final Stream<Object> eventStream = caseAggregate.updateEmployer(employer);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final EmployerUpdated employerUpdated = (EmployerUpdated) events.get(0);

        assertEmployer(employerUpdated, employer);
    }

    @Test
    public void shouldCreateEmployerUpdatedAndEmploymentStatusUpdatedEventsIfDefendantEmploymentStatusIsDifferentThanEmployed() {

        final UUID defendantId = createSjpCase().getDefendantId();
        final Employer employer = getEmployer(defendantId);
        final FinancialMeans financialMeans = getFinancialMeans(defendantId, "SELF-EMPLOYED");

        caseAggregate.updateFinancialMeans(financialMeans);

        final Stream<Object> eventStream = caseAggregate.updateEmployer(employer);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(2));

        final EmployerUpdated employerUpdated = (EmployerUpdated) events.get(0);
        final EmploymentStatusUpdated employmentStatusUpdated = (EmploymentStatusUpdated) events.get(1);

        assertEmployer(employerUpdated, employer);

        assertThat(employmentStatusUpdated.getDefendantId(), is(defendantId));
        assertThat(employmentStatusUpdated.getEmploymentStatus(), is("EMPLOYED"));
    }

    @Test
    public void shouldCreateDefendantNotFoundEventIfDefendantDoesNotExist() {
        final UUID defendantId = randomUUID();
        final Employer employer = getEmployer(defendantId);

        final Stream<Object> eventStream = caseAggregate.updateEmployer(employer);
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

    private Employer getEmployer(final UUID defendantId) {
        return new Employer(defendantId, "Burger King", "12345", "023402340234",
                new Address("street", "suburb", "town", "county", "ZY9 8 XW"));
    }

    private FinancialMeans getFinancialMeans(final UUID defendantId, final String employmentStatus) {
        return new FinancialMeans(defendantId, null, null, employmentStatus);
    }

    private void assertEmployer(final EmployerUpdated employerUpdated, final Employer sourceEmployer) {
        assertThat(employerUpdated.getDefendantId(), is(sourceEmployer.getDefendantId()));
        assertThat(employerUpdated.getName(), is(sourceEmployer.getName()));
        assertThat(employerUpdated.getEmployeeReference(), is(sourceEmployer.getEmployeeReference()));
        assertThat(employerUpdated.getPhone(), is(sourceEmployer.getPhone()));
        assertThat(employerUpdated.getAddress(), is(sourceEmployer.getAddress()));
    }

}
