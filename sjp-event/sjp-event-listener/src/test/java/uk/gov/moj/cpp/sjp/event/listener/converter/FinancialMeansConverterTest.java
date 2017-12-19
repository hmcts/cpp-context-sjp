package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.FORTNIGHTLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.MONTHLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.WEEKLY;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FinancialMeansConverterTest {

    @Parameterized.Parameter(0)
    public Income income;

    @Parameterized.Parameter(1)
    public Benefits benefits;

    @Parameterized.Parameter(2)
    public String employmentStatus;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new Income(FORTNIGHTLY, BigDecimal.valueOf(1.0)), new Benefits(true, "type", true), "EMPLOYED"},
                {new Income(MONTHLY, BigDecimal.valueOf(0)), new Benefits(false, "", false), "SELF_EMPLOYED"},
                {new Income(WEEKLY, null), new Benefits(null, null, false), "UNEMPLOYED"},
                {new Income(null, null), new Benefits(null, null, false), null},
        });
    }

    private FinancialMeansConverter financialMeansConverter;

    @Before
    public void init() {
        financialMeansConverter = new FinancialMeansConverter();
    }

    @Test
    public void shouldConvertToFinancialMeansEntity() {
        final FinancialMeansUpdated financialMeansUpdated = new FinancialMeansUpdated(UUID.randomUUID(),
                income, benefits, employmentStatus, null);


        final FinancialMeans financialMeans = financialMeansConverter.convertToFinancialMeansEntity(financialMeansUpdated);

        assertThat(financialMeans.getDefendantId(), equalTo(financialMeansUpdated.getDefendantId()));
        assertThat(financialMeans.getIncomePaymentFrequency(), equalTo(financialMeansUpdated.getIncome().getFrequency()));
        assertThat(financialMeans.getIncomePaymentAmount(), equalTo(financialMeansUpdated.getIncome().getAmount()));
        assertThat(financialMeans.getBenefitsClaimed(), equalTo(financialMeansUpdated.getBenefits().getClaimed()));
        assertThat(financialMeans.getBenefitsType(), equalTo(financialMeansUpdated.getBenefits().getType()));
        assertThat(financialMeans.getEmploymentStatus(), equalTo(financialMeansUpdated.getEmploymentStatus()));
    }
}
