package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class UpdateAllFinancialMeansTest extends CaseAggregateBaseTest {

    private UUID userId = UUID.randomUUID();

    @Test
    public void shouldCreateFinancialMeansUpdatedEventIfDefendantExists() {
        final UUID defendantId = caseReceivedEvent.getDefendant().getId();

        final Income income = new Income(IncomeFrequency.MONTHLY, BigDecimal.valueOf(1000.50));
        final Benefits benefits = new Benefits(false, EMPTY);

        final String employmentStatus = "EMPLOYED";
        final Employer employer = getEmployer(defendantId);
        final Stream<Object> eventStream = caseAggregate.updateAllFinancialMeans(userId,
                defendantId, income, benefits, employer, employmentStatus, null, null, null, false);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(4));

        final FinancialMeansUpdated financialMeansUpdated = (FinancialMeansUpdated) events.get(0);
        assertThat(financialMeansUpdated.getDefendantId(), equalTo(defendantId));
        assertThat(financialMeansUpdated.getBenefits(), equalTo(benefits));
        assertThat(financialMeansUpdated.getIncome(), equalTo(income));
        assertThat(financialMeansUpdated.getEmploymentStatus(), equalTo(employmentStatus));
        assertTrue(financialMeansUpdated.getOutgoings().isEmpty());

        final EmployerUpdated employerUpdated = (EmployerUpdated) events.get(1);
        assertThat(employerUpdated.getDefendantId(), equalTo(defendantId));
        assertThat(employerUpdated.getName(), equalTo(employer.getName()));
        assertThat(employerUpdated.getEmployeeReference(), equalTo(employer.getEmployeeReference()));
        assertThat(employerUpdated.getPhone(), equalTo(employer.getPhone()));
        assertThat(employerUpdated.getAddress(), equalTo(employer.getAddress()));

        final EmploymentStatusUpdated employmentStatusUpdated = (EmploymentStatusUpdated) events.get(2);
        assertThat(employmentStatusUpdated.getDefendantId(), equalTo(defendantId));
        assertThat(employmentStatusUpdated.getEmploymentStatus(), equalTo(employmentStatus));

    }

    @Test
    public void shouldCreateFinancialMeansUpdatedAndEmployerDeletedEvent() {
        final UUID defendantId = caseReceivedEvent.getDefendant().getId();

        final Income previousIncome = new Income(IncomeFrequency.MONTHLY, BigDecimal.valueOf(1000.50));
        final Benefits previousBenefits = new Benefits(false, EMPTY);
        final Employer previousEmployer = getEmployer(defendantId);
        final String previousEmploymentStatus = "EMPLOYED";

        final List<Object>  events = caseAggregate.updateAllFinancialMeans(userId,
                defendantId, previousIncome, previousBenefits, previousEmployer, previousEmploymentStatus, null, null, null, false).collect(toList());

        assertThat(events, hasSize(4));

        final String currentEmploymentStatus = "UNEMPLOYED";
        final List<Object>  newEvents = caseAggregate.updateAllFinancialMeans(userId,
                defendantId, previousIncome, previousBenefits, null, currentEmploymentStatus, null, null, null, false).collect(toList());

        assertThat(newEvents, hasSize(3));

        final FinancialMeansUpdated financialMeansUpdated = (FinancialMeansUpdated) newEvents.get(0);
        assertThat(financialMeansUpdated.getDefendantId(), equalTo(defendantId));
        assertThat(financialMeansUpdated.getBenefits(), equalTo(previousBenefits));
        assertThat(financialMeansUpdated.getIncome(), equalTo(previousIncome));
        assertThat(financialMeansUpdated.getEmploymentStatus(), equalTo(currentEmploymentStatus));
        assertTrue(financialMeansUpdated.getOutgoings().isEmpty());

        final EmployerDeleted employerUpdated = (EmployerDeleted) newEvents.get(1);
        assertThat(employerUpdated.getDefendantId(), equalTo(defendantId));
    }


    private static Employer getEmployer(final UUID defendantId) {
        return new Employer(defendantId, "Burger King", "12345", "023402340234",
                new Address("street", "suburb", "town", "county", "UK", "ZY9 8 XW"));
    }
}
