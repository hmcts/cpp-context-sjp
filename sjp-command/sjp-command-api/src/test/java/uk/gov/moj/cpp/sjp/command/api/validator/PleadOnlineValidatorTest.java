package uk.gov.moj.cpp.sjp.command.api.validator;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans.financialMeans;
import static uk.gov.justice.json.schemas.domains.sjp.command.Plea.GUILTY;
import static uk.gov.justice.json.schemas.domains.sjp.command.Plea.NOT_GUILTY;
import static uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline.pleadOnline;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.json.schemas.domains.sjp.command.Benefits;
import uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans;
import uk.gov.justice.json.schemas.domains.sjp.command.Frequency;
import uk.gov.justice.json.schemas.domains.sjp.command.Income;
import uk.gov.justice.json.schemas.domains.sjp.command.Offence;
import uk.gov.justice.json.schemas.domains.sjp.command.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.command.Plea;
import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

public class PleadOnlineValidatorTest {

    private static final Map<String, List<String>> ERROR_MISSING_FINANCIAL_MEANS = singletonMap("FinancialMeansRequiredWhenPleadingGuilty", singletonList("Financial Means are required when you are pleading GUILTY"));

    private static final Map<String, List<String>> ERROR_CASE_HAS_BEEN_REVIEWED = singletonMap(
            "CaseAlreadyReviewed",
            singletonList("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    private static final Map<String, List<String>> ERROR_PLEA_ALREADY_SUBMITTED = singletonMap(
            "PleaAlreadySubmitted",
            singletonList("Plea already submitted - Contact the Contact Centre if you need to change or discuss it"));


    private PleadOnlineValidator validatorUnderTest = new PleadOnlineValidator();

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(
                        GUILTY,
                        null,
                        equalTo(ERROR_MISSING_FINANCIAL_MEANS)
                ),
                Arguments.of(
                        GUILTY,
                        financialMeans().build(),
                        equalTo(ERROR_MISSING_FINANCIAL_MEANS)
                ),
                Arguments.of(
                        GUILTY,
                        financialMeans()
                                // .withoutBenefits
                                .withEmploymentStatus("EMPLOYED")
                                .withIncome(new Income(BigDecimal.TEN, Frequency.FORTNIGHTLY))
                                .build(),
                        equalTo(ERROR_MISSING_FINANCIAL_MEANS)
                ),
                Arguments.of(
                        GUILTY,
                        financialMeans()
                                .withBenefits(new Benefits(true, true, "Universal Credit"))
                                // .withoutEmploymentStatus()
                                .withIncome(new Income(BigDecimal.TEN, Frequency.FORTNIGHTLY))
                                .build(),
                        equalTo(ERROR_MISSING_FINANCIAL_MEANS)
                ),
                Arguments.of(
                        GUILTY,
                        financialMeans()
                                .withBenefits(new Benefits(true, true, "Universal Credit"))
                                .withEmploymentStatus("EMPLOYED")
                                // .withoutIncome()
                                .build(),

                        equalTo(ERROR_MISSING_FINANCIAL_MEANS)
                ),

                // POSITIVE SCENARIO - GUILTY
                Arguments.of(
                        GUILTY,
                        financialMeans()
                                .withBenefits(new Benefits(true, true, "Universal Credit"))
                                .withEmploymentStatus("EMPLOYED")
                                .withIncome(new Income(BigDecimal.TEN, Frequency.FORTNIGHTLY))
                                .build(),
                        is(emptyMap())
                ),

                // POSITIVE SCENARIO - NON_GUILTY
                Arguments.of(
                        NOT_GUILTY,
                        financialMeans().build(),
                        is(emptyMap())
                ),
                Arguments.of(
                        NOT_GUILTY,
                        null,
                        is(emptyMap())
                )
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldFailValidationWhenPleaGuiltyWithoutFinances(Plea inputPleaType, FinancialMeans inputFinancialMeans, Matcher<Map<String, List<String>>> expectedErrors) {
        final PleadOnline pleadOnline = buildPleadOnline(inputPleaType, inputFinancialMeans);

        final Map<String, List<String>> validate = validatorUnderTest.validate(pleadOnline);

        assertThat(validate, expectedErrors);
    }

    @Test
    public void shouldPassValidationWhenCaseHasProperStatus() {
        runValidationWithCaseDetailParameters(FALSE, FALSE, NO_PLEA_RECEIVED_READY_FOR_DECISION, FALSE, null, emptyMap());
    }

    @Test
    public void shouldFailValidationWhenCaseCompleted() {
        runValidationWithCaseDetailParameters(TRUE, FALSE, NO_PLEA_RECEIVED_READY_FOR_DECISION, FALSE, null, ERROR_CASE_HAS_BEEN_REVIEWED);
    }

    @Test
    public void shouldFailValidationWhenCaseAssigned() {
        runValidationWithCaseDetailParameters(FALSE, TRUE, NO_PLEA_RECEIVED_READY_FOR_DECISION, FALSE, null, ERROR_CASE_HAS_BEEN_REVIEWED);
    }

    @Test
    public void shouldFailValidationWhenCaseStatusCompleted() {
        runValidationWithCaseDetailParameters(FALSE, FALSE, COMPLETED, FALSE, null, ERROR_CASE_HAS_BEEN_REVIEWED);
    }

    @Test
    public void shouldFailValidationWhenCaseStatusReferredForCourtHearing() {
        runValidationWithCaseDetailParameters(FALSE, FALSE, REFERRED_FOR_COURT_HEARING, FALSE, null, ERROR_CASE_HAS_BEEN_REVIEWED);
    }

    @Test
    public void shouldFailValidationWhenPleadedPreviously() {
        runValidationWithCaseDetailParameters(FALSE, FALSE, NO_PLEA_RECEIVED_READY_FOR_DECISION, FALSE, GUILTY, ERROR_PLEA_ALREADY_SUBMITTED);
    }

    @Test
    public void shouldFailValidationWhenOffencePendingWithdrawal() {
        runValidationWithCaseDetailParameters(FALSE, FALSE, NO_PLEA_RECEIVED_READY_FOR_DECISION, TRUE, null, ERROR_CASE_HAS_BEEN_REVIEWED);
    }

    private void runValidationWithCaseDetailParameters(final Boolean completed, final Boolean assigned, final CaseStatus status, final Boolean pendingWithdrawal, final Plea plea, final Map<String, List<String>> error) {
        final JsonObject caseDetail = getCaseDetail(completed, assigned, status, pendingWithdrawal, plea);
        final Map<String, List<String>> validate = validatorUnderTest.validate(caseDetail);
        assertThat(validate, equalTo(error));
    }

    private JsonObject getCaseDetail(final Boolean completed, final Boolean assigned, final CaseStatus status, final Boolean pendingWithdrawal, final Plea plea) {

        final JsonObjectBuilder caseDetailBuilder = Json.createObjectBuilder()
                .add("id", UUID.randomUUID().toString());

        Optional.ofNullable(completed)
                .ifPresent(value -> caseDetailBuilder.add("completed", value));

        Optional.ofNullable(assigned)
                .ifPresent(value -> caseDetailBuilder.add("assigned", value));

        Optional.ofNullable(status)
                .ifPresent(value -> caseDetailBuilder.add("status", value.name()));

        final JsonObjectBuilder offenceObjectBuilder = Json.createObjectBuilder();

        Optional.ofNullable(pendingWithdrawal)
                .ifPresent(value -> offenceObjectBuilder.add("pendingWithdrawal", value));

        Optional.ofNullable(plea)
                .ifPresent(value -> offenceObjectBuilder.add("plea", value.name()));

        final JsonArray offences = Json.createArrayBuilder().add(offenceObjectBuilder.build()).build();

        final JsonObject defendant = Json.createObjectBuilder()
                .add("offences", offences)
                .build();

        caseDetailBuilder.add("defendant", defendant);

        return caseDetailBuilder.build();
    }

    private static PleadOnline buildPleadOnline(final Plea plea, final FinancialMeans financialMeans) {
        return pleadOnline()
                .withPersonalDetails(
                        PersonalDetails.personalDetails()
                                .withFirstName("first name")
                                .withLastName("last name")
                                .build()
                )
                .withOffences(singletonList(Offence.offence()
                        .withPlea(plea)
                        .build()))
                .withFinancialMeans(financialMeans)
                .build();
    }

}