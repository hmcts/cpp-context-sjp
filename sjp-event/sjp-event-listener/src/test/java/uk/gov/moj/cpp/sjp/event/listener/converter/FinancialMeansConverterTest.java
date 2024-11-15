package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.FORTNIGHTLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.MONTHLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.WEEKLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.YEARLY;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class FinancialMeansConverterTest {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(new Income(FORTNIGHTLY, BigDecimal.valueOf(1.0)), new Benefits(true, "type", true), "EMPLOYED",BigDecimal.valueOf(1.0)),
                Arguments.of(new Income(MONTHLY, BigDecimal.valueOf(0)), new Benefits(false, "", false), "SELF_EMPLOYED", BigDecimal.valueOf(0)),
                Arguments.of(new Income(WEEKLY, null), new Benefits(null, null, false), "UNEMPLOYED", null),
                Arguments.of(new Income(null, null), new Benefits(null, null, false), null, null),
                Arguments.of(new Income(YEARLY, BigDecimal.valueOf(999999999.99)), new Benefits(true, "type", true), "EMPLOYED", BigDecimal.valueOf(999999999.99)),
                Arguments.of(new Income(YEARLY, BigDecimal.valueOf(1352740137)), new Benefits(true, "type", true), "EMPLOYED", BigDecimal.valueOf(0))
        );
    }

    private FinancialMeansConverter financialMeansConverter;

    @BeforeEach
    public void init() {
        financialMeansConverter = new FinancialMeansConverter();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertToFinancialMeansEntity(Income income, Benefits benefits, String employmentStatus, final BigDecimal incomeAmount) {
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEvent(UUID.randomUUID(),
                income, benefits, employmentStatus, null, null, null, null);

        final FinancialMeans financialMeans = financialMeansConverter.convertToFinancialMeansEntity(financialMeansUpdated);

        assertThat(financialMeans.getDefendantId(), equalTo(financialMeansUpdated.getDefendantId()));
        assertThat(financialMeans.getIncomePaymentFrequency(), equalTo(financialMeansUpdated.getIncome().getFrequency()));
        assertThat(financialMeans.getIncomePaymentAmount(), equalTo(incomeAmount));
        assertThat(financialMeans.getBenefitsClaimed(), equalTo(financialMeansUpdated.getBenefits().getClaimed()));
        assertThat(financialMeans.getBenefitsType(), equalTo(financialMeansUpdated.getBenefits().getType()));
        assertThat(financialMeans.getEmploymentStatus(), equalTo(financialMeansUpdated.getEmploymentStatus()));
    }
}
