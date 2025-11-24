package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.MONTHLY;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.WEEKLY;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.math.BigDecimal;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateFinancialMeanIT extends BaseIntegrationTest {

    private FinancialMeansHelper financialMeansHelper;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @BeforeEach
    public void setUp() {
        financialMeansHelper = new FinancialMeansHelper();
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "NATIONAL_COURT_CODE", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("NATIONAL_COURT_CODE", "TestRegion");
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
    }

    @AfterEach
    public void tearDown() {
        financialMeansHelper.close();
    }

    @Test
    public void shouldUpdateAndFetchFinancialMeansWithoutOptionalFields() {

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");

        final JsonObject payload = createObjectBuilder()
                .add("income", createObjectBuilder())
                .add("benefits", createObjectBuilder())
                .build();

        final Matcher<Object> expectedFinancialMeansMatcher = isJson(allOf(
                withJsonPath("$.defendantId", is(defendantId)),
                withoutJsonPath("$.income.frequency"),
                withoutJsonPath("$.income.amount"),
                withoutJsonPath("$.benefits.claimed"),
                withoutJsonPath("$.benefits.type")
        ));

        financialMeansHelper.updateFinancialMeans(caseId, defendantId, payload);
        financialMeansHelper.getFinancialMeans(defendantId, expectedFinancialMeansMatcher);
        financialMeansHelper.getEventFromPublicTopic(expectedFinancialMeansMatcher);
    }

    @Test
    public void shouldAddUpdateAndFetchFinancialMeans() {

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        final Income originalIncome = new Income(MONTHLY, BigDecimal.valueOf(1000.50));
        final Income updatedIncome = new Income(WEEKLY, BigDecimal.valueOf(200.0));
        final Benefits updatedBenefits = new Benefits(false, "", null);
        final String employmentStatus = "EMPLOYED";

        final JsonObject original = createObjectBuilder()
                .add("income", createObjectBuilder()
                        .add("frequency", originalIncome.getFrequency().name())
                        .add("amount", originalIncome.getAmount()))
                .add("benefits", createObjectBuilder())
                .add("employmentStatus", employmentStatus)
                .build();

        final JsonObject updated = createObjectBuilder()
                .add("income", createObjectBuilder()
                        .add("frequency", updatedIncome.getFrequency().name())
                        .add("amount", updatedIncome.getAmount()))
                .add("benefits", createObjectBuilder()
                        .add("claimed", updatedBenefits.getClaimed())
                        .add("type", updatedBenefits.getType()))
                .build();

        final Matcher<Object> expectedOriginal = isJson(allOf(
                withJsonPath("$.income.frequency", is(originalIncome.getFrequency().name())),
                withJsonPath("$.income.amount", is(originalIncome.getAmount().doubleValue())),
                withoutJsonPath("$.benefits.claimed"),
                withoutJsonPath("$.benefits.type"),
                withJsonPath("$.employmentStatus", is(employmentStatus))
        ));

        final Matcher<Object> expectedUpdated = isJson(allOf(
                withJsonPath("$.income.frequency", is(updatedIncome.getFrequency().name())),
                withJsonPath("$.income.amount", is(updatedIncome.getAmount().doubleValue())),
                withJsonPath("$.benefits.claimed", is(updatedBenefits.getClaimed())),
                withJsonPath("$.benefits.type", is(updatedBenefits.getType())),
                withoutJsonPath("$.employmentStatus")
        ));

        financialMeansHelper.updateFinancialMeans(caseId, defendantId, original);
        financialMeansHelper.getFinancialMeans(defendantId, expectedOriginal);
        financialMeansHelper.getEventFromPublicTopic(expectedOriginal);

        financialMeansHelper.updateFinancialMeans(caseId, defendantId, updated);
        financialMeansHelper.getFinancialMeans(defendantId, expectedUpdated);
        financialMeansHelper.getEventFromPublicTopic(expectedUpdated);
    }

    @Test
    public void shouldReturnEmptyObjectWhenFinancialMeansDoNotExist() {
        final UUID nonExistingDefendantId = randomUUID();
        final Response response = financialMeansHelper.getFinancialMeans(nonExistingDefendantId.toString());
        assertThat(response.readEntity(String.class), is("{}"));
    }

}
