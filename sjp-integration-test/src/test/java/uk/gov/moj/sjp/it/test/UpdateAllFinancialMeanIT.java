package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.MONTHLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.YEARLY;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.getEmployerDeletedPublicEventMatcher;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.getEmployerPayload;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.getEmployerUpdatedPayloadMatcher;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.getEmployerUpdatedPublicEventMatcher;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.pollForEmployerForDefendant;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.math.BigDecimal;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateAllFinancialMeanIT extends BaseIntegrationTest {

    final private FinancialMeansHelper financialMeansHelper = new FinancialMeansHelper();
    final private FinancialMeansHelper allFinancialMeansHelper = new FinancialMeansHelper("public.sjp.all-financial-means-updated");
    final private EmployerHelper employerHelper = new EmployerHelper();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private static final String NATIONAL_COURT_CODE = "1080";

    @BeforeEach
    public void setUp() {
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();

        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");
        createCaseForPayloadBuilder(this.createCasePayloadBuilder);
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
    }

    @AfterEach
    public void tearDown() {
        financialMeansHelper.close();
    }

    @Test
    public void shouldUpdateFinancialMeansWithEmployerDetails() {

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        final Income income = new Income(MONTHLY, BigDecimal.valueOf(1000.50));
        final Benefits benefits = new Benefits(false, "", null);
        final String employmentStatus = "EMPLOYED";
        final JsonObject employer = getEmployerPayload();

        final JsonObject payload = createObjectBuilder()
                .add("income", createObjectBuilder()
                        .add("frequency", income.getFrequency().name())
                        .add("amount", income.getAmount()))
                .add("benefits", createObjectBuilder()
                        .add("claimed", benefits.getClaimed())
                        .add("type", benefits.getType()))
                .add("employment", createObjectBuilder()
                        .add("status", employmentStatus))
                .add("employer", employer)
                .build();

        final Matcher<Object> expected = isJson(allOf(
                withJsonPath("$.income.frequency", is(income.getFrequency().name())),
                withJsonPath("$.income.amount", is(income.getAmount().doubleValue())),
                withJsonPath("$.benefits.claimed", is(benefits.getClaimed())),
                withJsonPath("$.benefits.type", is(benefits.getType())),
                withJsonPath("$.employmentStatus", is(employmentStatus))
        ));

        financialMeansHelper.updateAllFinancialMeans(caseId, defendantId, payload);
        financialMeansHelper.getFinancialMeans(defendantId, expected);
        financialMeansHelper.getEventFromPublicTopic(expected);

        shouldGetAllFinancialMeansUpdatedEventFromPublicTopic(defendantId);

        // assert the employer details
        pollForEmployerForDefendant(defendantId, getEmployerUpdatedPayloadMatcher(employer));
        assertThat(employerHelper.getEventFromPublicTopic(), getEmployerUpdatedPublicEventMatcher(employer));
    }

    @Test
    public void shouldDeleteEmployerDetails() {

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        final Income incomeBefore = new Income(MONTHLY, BigDecimal.valueOf(1000.51));
        final Benefits benefitsBefore = new Benefits(false, "", null);
        final String employmentStatusBefore = "EMPLOYED";
        final JsonObject employerBefore = getEmployerPayload();

        final JsonObject payloadBefore = createObjectBuilder()
                .add("income", createObjectBuilder()
                        .add("frequency", incomeBefore.getFrequency().name())
                        .add("amount", incomeBefore.getAmount()))
                .add("benefits", createObjectBuilder()
                        .add("claimed", benefitsBefore.getClaimed())
                        .add("type", benefitsBefore.getType()))
                .add("employment", createObjectBuilder()
                        .add("status", employmentStatusBefore))
                .add("employer", employerBefore)
                .build();

        final Matcher<Object> expectedBefore = isJson(allOf(
                withJsonPath("$.income.frequency", is(incomeBefore.getFrequency().name())),
                withJsonPath("$.income.amount", is(incomeBefore.getAmount().doubleValue())),
                withJsonPath("$.benefits.claimed", is(benefitsBefore.getClaimed())),
                withJsonPath("$.benefits.type", is(benefitsBefore.getType())),
                withJsonPath("$.employmentStatus", is(employmentStatusBefore))
        ));

        // update the financial means
        financialMeansHelper.updateAllFinancialMeans(caseId, defendantId, payloadBefore);

        financialMeansHelper.getFinancialMeans(defendantId, expectedBefore);
        financialMeansHelper.getEventFromPublicTopic(expectedBefore);

        // assert the employer details
        pollForEmployerForDefendant(defendantId, getEmployerUpdatedPayloadMatcher(employerBefore));
        assertThat(employerHelper.getEventFromPublicTopic(), getEmployerUpdatedPublicEventMatcher(employerBefore));


        final Income incomeAfter = new Income(YEARLY, BigDecimal.valueOf(10000.52));
        final Benefits benefitsAfter = new Benefits(false, "NONE", null);
        final String employmentStatusAfter = "UNEMPLOYED";

        final JsonObject payloadAfter = createObjectBuilder()
                .add("income", createObjectBuilder()
                        .add("frequency", incomeAfter.getFrequency().name())
                        .add("amount", incomeAfter.getAmount()))
                .add("benefits", createObjectBuilder()
                        .add("claimed", benefitsAfter.getClaimed())
                        .add("type", benefitsAfter.getType()))
                .add("employment", createObjectBuilder()
                        .add("status", employmentStatusAfter))
                .build();
        financialMeansHelper.updateAllFinancialMeans(caseId, defendantId, payloadAfter);

        final Matcher<Object> expectedAfter = isJson(allOf(
                withJsonPath("$.income.frequency", is(incomeAfter.getFrequency().name())),
                withJsonPath("$.income.amount", is(incomeAfter.getAmount().doubleValue())),
                withJsonPath("$.benefits.claimed", is(benefitsAfter.getClaimed())),
                withJsonPath("$.benefits.type", is(benefitsAfter.getType())),
                withJsonPath("$.employmentStatus", is(employmentStatusAfter))
        ));

        financialMeansHelper.getFinancialMeans(defendantId, expectedAfter);
        financialMeansHelper.getEventFromPublicTopic(expectedAfter);

        shouldGetAllFinancialMeansUpdatedEventFromPublicTopic(defendantId);
        // assert the employer details
        pollForEmployerForDefendant(defendantId, isJson(withJsonPath("$.size()", is(0))));
        assertThat(employerHelper.getEventFromPublicTopic(), getEmployerDeletedPublicEventMatcher(defendantId));
    }

    private void shouldGetAllFinancialMeansUpdatedEventFromPublicTopic(final String defendantId) {
        final Matcher<Object> jsonMatcher = isJson(allOf(
                withJsonPath("$.defendantId", is(defendantId))
        ));
        allFinancialMeansHelper.getEventFromPublicTopic(jsonMatcher);
    }
}
