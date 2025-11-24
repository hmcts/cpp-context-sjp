package uk.gov.moj.cpp.sjp.query.view.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.FORTNIGHTLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.MONTHLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.WEEKLY;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;

public class FinancialMeansConverterTest {

    
    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(new Income(FORTNIGHTLY, BigDecimal.valueOf(1.0)), new Benefits(true, "type", null), "EMPLOYED"),
                Arguments.of(new Income(MONTHLY, BigDecimal.valueOf(0)), new Benefits(false, "", null),"SELF_EMPLOYED"),
                Arguments.of(new Income(WEEKLY, null), new Benefits(null, null, null), "UNEMPLOYED"),
                Arguments.of(new Income(null, null), new Benefits(null, null, null), null)
        );
    }

    private FinancialMeansConverter financialMeansConverter;

    @BeforeEach
    public void init() {
        financialMeansConverter = new FinancialMeansConverter();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertToFinancialMeans(Income income, Benefits benefits, String employmentStatus) {
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
