package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.plea.EmploymentStatus;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class UpdateEmployerTest {

    private CaseAggregate caseAggregate;

    private UUID defendantId;

    private Employer employer;

    private Clock clock = new UtcClock();

    private UUID userId = UUID.randomUUID();

    @Before
    public void init() {
        caseAggregate = new CaseAggregate();
        defendantId = receiveCase().getDefendant().getId();
        employer = getEmployer(defendantId);
    }

    @Test
    public void shouldCreateEmployerUpdatedAndEmploymentStatusUpdatedEventsIfDefendantDoesNotHaveEmploymentStatus() {
        final Stream<Object> eventStream = caseAggregate.updateEmployer(userId, employer);
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
        final FinancialMeans financialMeans = getFinancialMeans(defendantId, EmploymentStatus.EMPLOYED);

        caseAggregate.updateFinancialMeans(userId, financialMeans);

        final Stream<Object> eventStream = caseAggregate.updateEmployer(userId, employer);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final EmployerUpdated employerUpdated = (EmployerUpdated) events.get(0);
        assertEmployer(employerUpdated, employer);
    }

    @Test
    public void shouldCreateEmployerUpdatedAndEmploymentStatusUpdatedEventsIfDefendantEmploymentStatusIsDifferentThanEmployed() {
        final FinancialMeans financialMeans = getFinancialMeans(defendantId, EmploymentStatus.SELF_EMPLOYED);

        caseAggregate.updateFinancialMeans(userId, financialMeans);

        final Stream<Object> eventStream = caseAggregate.updateEmployer(userId, employer);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(2));

        final EmployerUpdated employerUpdated = (EmployerUpdated) events.get(0);
        assertEmployer(employerUpdated, employer);

        final EmploymentStatusUpdated employmentStatusUpdated = (EmploymentStatusUpdated) events.get(1);
        assertThat(employmentStatusUpdated.getDefendantId(), is(defendantId));
        assertThat(employmentStatusUpdated.getEmploymentStatus(), is(EmploymentStatus.EMPLOYED.name()));
    }

    private CaseReceived receiveCase() {
        final Case sjpCase = CaseBuilder.aDefaultSjpCase().build();
        final Stream<Object> caseCreatedEvents = caseAggregate.receiveCase(sjpCase, clock.now());
        return caseCreatedEvents.filter(CaseReceived.class::isInstance)
                .map(CaseReceived.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(format("No event of type %s found.", CaseReceived.class.getSimpleName())));
    }

    private static Employer getEmployer(final UUID defendantId) {
        return new Employer(defendantId, "Burger King", "12345", "023402340234",
                new Address("street", "suburb", "town", "county", "UK", "ZY9 8 XW"));
    }

    private static FinancialMeans getFinancialMeans(final UUID defendantId, final EmploymentStatus employmentStatus) {
        return new FinancialMeans(defendantId, null, null, employmentStatus.name(), null, null, null, null);
    }

    private void assertEmployer(final EmployerUpdated employerUpdated, final Employer sourceEmployer) {
        assertThat(employerUpdated.getDefendantId(), is(sourceEmployer.getDefendantId()));
        assertThat(employerUpdated.getName(), is(sourceEmployer.getName()));
        assertThat(employerUpdated.getEmployeeReference(), is(sourceEmployer.getEmployeeReference()));
        assertThat(employerUpdated.getPhone(), is(sourceEmployer.getPhone()));
        assertThat(employerUpdated.getAddress(), is(sourceEmployer.getAddress()));
    }

}
