package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNull;

import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class UpdateFinancialMeansTest {

    private CaseAggregate caseAggregate;
    private Income income;
    private Benefits benefits;
    private List<Outgoing> outgoings;

    @Before
    public void init() {
        caseAggregate = new CaseAggregate();
        income = new Income(IncomeFrequency.MONTHLY, BigDecimal.valueOf(1000.50));
        benefits = new Benefits(false, "", null);
        outgoings = new ArrayList<>();
        outgoings.add(new Outgoing("food", BigDecimal.valueOf(300.2)));
        outgoings.add(new Outgoing("travel", BigDecimal.valueOf(100,8)));
    }

    @Test
    public void shouldCreateFinancialMeansUpdatedEventIfDefendantExists() {
        final Case aCase = CaseBuilder.aDefaultSjpCase().build();
        final Stream<Object> eventsStream = caseAggregate.createCase(aCase, ZonedDateTime.now());
        final SjpCaseCreated sjpCaseCreated  = (SjpCaseCreated) eventsStream.findFirst().get();

        final UUID defendantId = sjpCaseCreated.getDefendantId();
        final FinancialMeans financialMeans = new FinancialMeans(defendantId, income, benefits, "EMPLOYED");

        final Stream<Object> eventStream = caseAggregate.updateFinancialMeans(financialMeans);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final FinancialMeansUpdated financialMeansUpdated = (FinancialMeansUpdated) events.get(0);

        assertThat(financialMeansUpdated.getDefendantId(), equalTo(defendantId));
        assertThat(financialMeansUpdated.getBenefits(), equalTo(benefits));
        assertThat(financialMeansUpdated.getIncome(), equalTo(income));
        assertThat(financialMeansUpdated.getEmploymentStatus(), equalTo(financialMeans.getEmploymentStatus()));
        assertNull(financialMeansUpdated.getOutgoings());
    }

    @Test
    public void shouldCreateDefendantNotFoundEventIfDefendantDoesNotExist() {
        final UUID defendantId = UUID.randomUUID();
        final FinancialMeans financialMeans = new FinancialMeans(defendantId, income, benefits, "EMPLOYED");

        final Stream<Object> eventStream = caseAggregate.updateFinancialMeans(financialMeans);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final DefendantNotFound defendantNotFound = (DefendantNotFound) events.get(0);

        assertThat(defendantNotFound.getDefendantId(), equalTo(defendantId.toString()));
        assertThat(defendantNotFound.getDescription(), equalTo("Update financial means"));
    }
}
