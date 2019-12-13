package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.AllFinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateAllFinancialMeansHandlerTest {

    final private UpdateAllFinancialMeansAggregateHandler updateAllFinancialMeansHandler = UpdateAllFinancialMeansAggregateHandler.INSTANCE;

    @Test
    public void shouldCreateFinancialMeansAndEmployerCreatedEvents() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();
        final UUID defendantId = randomUUID();
        final Income income = new Income(IncomeFrequency.YEARLY, new BigDecimal(100.5));
        final Benefits benefits = new Benefits(true, "");
        final Employer employer = getEmployer(defendantId);
        final String employmentStatus = "EMPLOYED";

        final CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(caseId);
        caseAggregateState.addOffenceIdsForDefendant(defendantId, new HashSet<>());

        final List<Object> eventList = updateAllFinancialMeansHandler.updateAllFinancialMeans(userId,
                defendantId,
                income,
                benefits,
                employer,
                employmentStatus,
                caseAggregateState).collect(Collectors.toList());

        assertThat(eventList.size(), is(4));
        assertThat(eventList.get(0), samePropertyValuesAs(FinancialMeansUpdated.createEvent(defendantId, income, benefits, employmentStatus)));
        assertThat(eventList.get(1), samePropertyValuesAs(EmployerUpdated.createEvent(defendantId, employer)));
        assertThat(eventList.get(2), samePropertyValuesAs(new EmploymentStatusUpdated(defendantId, employmentStatus)));
        assertThat(eventList.get(3), samePropertyValuesAs(new AllFinancialMeansUpdated(defendantId, income, benefits, employmentStatus,employer)));
    }

    @Test
    public void shouldCreateFinancialMeansAndEmployerDeletedEvents() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();
        final UUID defendantId = randomUUID();
        final Income income = new Income(IncomeFrequency.YEARLY, new BigDecimal(100.5));
        final Benefits benefits = new Benefits(true, "");
        final String employmentStatus = "UNEMPLOYED";

        final CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(caseId);
        caseAggregateState.addOffenceIdsForDefendant(defendantId, new HashSet<>());
        caseAggregateState.updateEmploymentStatusForDefendant(defendantId, employmentStatus);
        final List<Object> eventList = updateAllFinancialMeansHandler.updateAllFinancialMeans(userId,
                defendantId,
                income,
                benefits,
                null,
                employmentStatus,
                caseAggregateState).collect(Collectors.toList());

        assertThat(eventList.size(), is(3));
        assertThat(eventList.get(0), samePropertyValuesAs(FinancialMeansUpdated.createEvent(defendantId, income, benefits, employmentStatus)));
        assertThat(eventList.get(1), samePropertyValuesAs(new EmployerDeleted(defendantId)));
        assertThat(eventList.get(2), samePropertyValuesAs(new AllFinancialMeansUpdated(defendantId, income, benefits, employmentStatus,null)));

    }

    private static Employer getEmployer(final UUID defendantId) {
        return new Employer(defendantId, "Burger King", "12345", "023402340234",
                new Address("street", "suburb", "town", "county", "UK", "ZY9 8 XW"));
    }

}