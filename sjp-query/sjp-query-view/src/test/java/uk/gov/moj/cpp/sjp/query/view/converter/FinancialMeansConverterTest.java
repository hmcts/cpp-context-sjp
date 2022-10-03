package uk.gov.moj.cpp.sjp.query.view.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.FORTNIGHTLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.MONTHLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.WEEKLY;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

import java.math.BigDecimal;
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
                {new Income(FORTNIGHTLY, BigDecimal.valueOf(1.0)), new Benefits(true, "type", null), "EMPLOYED"},
                {new Income(MONTHLY, BigDecimal.valueOf(0)), new Benefits(false, "", null),"SELF_EMPLOYED"},
                {new Income(WEEKLY, null), new Benefits(null, null, null), "UNEMPLOYED"},
                {new Income(null, null), new Benefits(null, null, null), null},
        });
    }

    private FinancialMeansConverter financialMeansConverter;

    @Before
    public void init() {
        financialMeansConverter = new FinancialMeansConverter();
    }

    @Test
    public void shouldConvertToFinancialMeans() {
        final FinancialMeans financialMeansEntity = new FinancialMeans(UUID.randomUUID(),
                income.getFrequency(), income.getAmount(), benefits.getClaimed(), benefits.getType(), employmentStatus);


        final uk.gov.moj.cpp.sjp.domain.FinancialMeans financialMeans = financialMeansConverter.convertToFinancialMeans(financialMeansEntity, null);

        assertThat(financialMeans.getDefendantId(), equalTo(financialMeansEntity.getDefendantId()));
        if(financialMeans.getIncome() != null) {
            assertThat(financialMeans.getIncome().getFrequency(), equalTo(financialMeansEntity.getIncomePaymentFrequency()));
            assertThat(financialMeans.getIncome().getAmount(), equalTo(financialMeansEntity.getIncomePaymentAmount()));
        }
        if(financialMeans.getBenefits() != null) {
            assertThat(financialMeans.getBenefits().getClaimed(), equalTo(financialMeansEntity.getBenefitsClaimed()));
            assertThat(financialMeans.getBenefits().getType(), equalTo(financialMeansEntity.getBenefitsType()));
        }
        assertThat(financialMeans.getEmploymentStatus(), equalTo(financialMeansEntity.getEmploymentStatus()));
    }
}
