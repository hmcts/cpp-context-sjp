package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;

public class CaseFinancialMeansHandlerTest  {

    private static final CaseAggregateState caseAggregateState = new CaseAggregateState();

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();


    @Test
    public void shouldReviseTheIncomeAmountIfItIsMoreThanThreshold() {
        final BigDecimal incomeAmount  = BigDecimal.valueOf(1352740137);
        final FinancialMeans financialMeans = buildFinancialMeans(incomeAmount);

        final Stream<Object> eventStream =  CaseFinancialMeansHandler.INSTANCE.updateFinancialMeans(USER_ID, financialMeans, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final FinancialMeansUpdated  financialMeansUpdated =  (FinancialMeansUpdated) eventList.get(0);

        assertThat(0, is(financialMeansUpdated.getIncome().getAmount().intValue()));
    }

    @Test
    public void shouldNotReviseTheIncomeAmountIfItIsEqualThreshold() {
        final BigDecimal incomeAmount  = BigDecimal.valueOf(999999999.99);
        final FinancialMeans financialMeans = buildFinancialMeans(incomeAmount);

        final Stream<Object> eventStream =  CaseFinancialMeansHandler.INSTANCE.updateFinancialMeans(USER_ID, financialMeans, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final FinancialMeansUpdated  financialMeansUpdated =  (FinancialMeansUpdated) eventList.get(0);

        assertThat(incomeAmount, is(financialMeansUpdated.getIncome().getAmount()));
    }

    @Test
    public void shouldNotReviseTheIncomeAmountIfItIsLessThanThreshold() {
        final BigDecimal incomeAmount  = BigDecimal.valueOf(200000.98);
        final FinancialMeans financialMeans = buildFinancialMeans(incomeAmount);

        final Stream<Object> eventStream =  CaseFinancialMeansHandler.INSTANCE.updateFinancialMeans(USER_ID, financialMeans, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final FinancialMeansUpdated  financialMeansUpdated =  (FinancialMeansUpdated) eventList.get(0);

        assertThat(incomeAmount, is(financialMeansUpdated.getIncome().getAmount()));
    }

    private static FinancialMeans buildFinancialMeans(final BigDecimal incomeAmount) {
        final Income income = new Income(IncomeFrequency.YEARLY, incomeAmount);
        final FinancialMeans financialMeans = new FinancialMeans(DEFENDANT_ID,
                income,
                new Benefits(),
                "EMPLOYED",
                null,
                null,
                0,
                Boolean.FALSE);

        caseAggregateState.setCaseId(CASE_ID);
        caseAggregateState.addOffenceIdsForDefendant(DEFENDANT_ID, new HashSet<>());
        return financialMeans;
    }
}
