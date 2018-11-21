package uk.gov.moj.cpp.sjp.command.api.validator;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans.financialMeans;
import static uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline.pleadOnline;

import uk.gov.justice.json.schemas.domains.sjp.command.Benefits;
import uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans;
import uk.gov.justice.json.schemas.domains.sjp.command.Frequency;
import uk.gov.justice.json.schemas.domains.sjp.command.Income;
import uk.gov.justice.json.schemas.domains.sjp.command.Offence;
import uk.gov.justice.json.schemas.domains.sjp.command.Plea;
import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PleadOnlineValidatorTest {

    private static final Map<String, List<String>> ERROR_MISSING_FINANCIAL_MEANS = singletonMap("financialMeans", singletonList("Financial Means are required when you are pleading GUILTY"));

    private PleadOnlineValidator validatorUnderTest = new PleadOnlineValidator();

    @Parameter(0)
    public Plea inputPleaType;

    @Parameter(1)
    public FinancialMeans inputFinancialMeans;

    @Parameter(2)
    public Matcher<Map<String, List<String>>> expectedErrors;

    @Parameters(name = "PleaOnline as {0} with FinancialMeans={1} will give errors={2}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {
                    Plea.GUILTY,
                    null,
                    equalTo(ERROR_MISSING_FINANCIAL_MEANS)
                },
                {
                    Plea.GUILTY,
                    financialMeans().build(),
                    equalTo(ERROR_MISSING_FINANCIAL_MEANS)
                },
                {
                    Plea.GUILTY,
                    financialMeans()
                        // .withoutBenefits
                        .withEmploymentStatus("EMPLOYED")
                        .withIncome(new Income(BigDecimal.TEN, Frequency.FORTNIGHTLY))
                        .build(),
                    equalTo(ERROR_MISSING_FINANCIAL_MEANS),
                },
                {
                    Plea.GUILTY,
                    financialMeans()
                        .withBenefits(new Benefits(true, true, "Universal Credit"))
                        // .withoutEmploymentStatus()
                        .withIncome(new Income(BigDecimal.TEN, Frequency.FORTNIGHTLY))
                        .build(),
                    equalTo(ERROR_MISSING_FINANCIAL_MEANS)
                },
                {
                    Plea.GUILTY,
                    financialMeans()
                            .withBenefits(new Benefits(true, true, "Universal Credit"))
                            .withEmploymentStatus("EMPLOYED")
                            // .withoutIncome()
                            .build(),

                    equalTo(ERROR_MISSING_FINANCIAL_MEANS)
                },

                // POSITIVE SCENARIO - GUILTY
                {
                    Plea.GUILTY,
                        financialMeans()
                                .withBenefits(new Benefits(true, true, "Universal Credit"))
                                .withEmploymentStatus("EMPLOYED")
                                .withIncome(new Income(BigDecimal.TEN, Frequency.FORTNIGHTLY))
                                .build(),
                    is(emptyMap())
                },

                // POSITIVE SCENARIO - NON_GUILTY
                {
                    Plea.NOT_GUILTY,
                    financialMeans().build(),
                    is(emptyMap())
                },
                {
                    Plea.NOT_GUILTY,
                    null,
                    is(emptyMap())
                },

        });
    }

    @Test
    public void shouldFailValidationWhenPleaGuiltyWithoutFinanes() {
        final PleadOnline pleadOnline = buildPleadOnline(inputPleaType, inputFinancialMeans);

        final Map<String, List<String>> validate = validatorUnderTest.validate(pleadOnline);

        assertThat(validate, expectedErrors);
    }

    private static PleadOnline buildPleadOnline(final Plea plea, final FinancialMeans financialMeans) {
        return pleadOnline()
                .withOffences(singletonList(Offence.offence()
                        .withPlea(plea)
                        .build()))
                .withFinancialMeans(financialMeans)
                .build();
    }

}