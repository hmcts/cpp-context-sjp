package uk.gov.moj.cpp.sjp.event.listener.converter;

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
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;

public class FinancialMeansConverterTest {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(new Income(FORTNIGHTLY, BigDecimal.valueOf(1.0)), new Benefits(true, "type", true), "EMPLOYED"),
                Arguments.of(new Income(MONTHLY, BigDecimal.valueOf(0)), new Benefits(false, "", false), "SELF_EMPLOYED"),
                Arguments.of(new Income(WEEKLY, null), new Benefits(null, null, false), "UNEMPLOYED"),
                Arguments.of(new Income(null, null), new Benefits(null, null, false), null)
        );
    }

    private FinancialMeansConverter financialMeansConverter;

    @BeforeEach
    public void init() {
        financialMeansConverter = new FinancialMeansConverter();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertToFinancialMeansEntity(Income income, Benefits benefits, String employmentStatus) {
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEvent(UUID.randomUUID(),
                income, benefits, employmentStatus, null, null, null, null);

        final FinancialMeans financialMeans = financialMeansConverter.convertToFinancialMeansEntity(financialMeansUpdated);

        assertThat(financialMeans.getDefendantId(), equalTo(financialMeansUpdated.getDefendantId()));
        assertThat(financialMeans.getIncomePaymentFrequency(), equalTo(financialMeansUpdated.getIncome().getFrequency()));
        assertThat(financialMeans.getIncomePaymentAmount(), equalTo(financialMeansUpdated.getIncome().getAmount()));
        assertThat(financialMeans.getBenefitsClaimed(), equalTo(financialMeansUpdated.getBenefits().getClaimed()));
        assertThat(financialMeans.getBenefitsType(), equalTo(financialMeansUpdated.getBenefits().getType()));
        assertThat(financialMeans.getEmploymentStatus(), equalTo(financialMeansUpdated.getEmploymentStatus()));
    }
}
