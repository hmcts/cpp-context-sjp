package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_ADDRESS;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_ADDRESS_1;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_ADDRESS_2;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_ADDRESS_3;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_ADDRESS_4;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_ADDRESS_5;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_EMPLOYEE_REFERENCE;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_NAME;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_PHONE;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.FIELD_POST_CODE;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmployerIT extends BaseIntegrationTest {

    private EmployerHelper employerHelper;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private FinancialMeansHelper financialMeansHelper;


    @Before
    public void setUp() {
        employerHelper = new EmployerHelper();
        financialMeansHelper = new FinancialMeansHelper();
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

    @After
    public void tearDown() throws Exception {
        employerHelper.close();
        financialMeansHelper.close();
    }

    @Test
    public void shouldCreateUpdateAndDeleteEmployer() {

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");

        final JsonObject employer1 = getEmployerPayload();

        final JsonObject employer2 = getEmployerPayload();

        employerHelper.updateEmployer(caseId, defendantId, employer1);
        employerHelper.getEmployer(defendantId, employerHelper.getEmployerUpdatedPayloadMatcher(employer1));
        assertThat(employerHelper.getEventFromPublicTopic(), employerHelper.getEmployerUpdatedPublicEventMatcher(employer1));

        employerHelper.updateEmployer(caseId, defendantId, employer2);
        employerHelper.getEmployer(defendantId, employerHelper.getEmployerUpdatedPayloadMatcher(employer2));

        assertThat(employerHelper.getEventFromPublicTopic(), employerHelper.getEmployerUpdatedPublicEventMatcher(employer2));

        final Matcher<Object> expectedFinancialMeans = isJson(withJsonPath("$.employmentStatus", is("EMPLOYED")));
        financialMeansHelper.getFinancialMeans(defendantId, expectedFinancialMeans);

        employerHelper.deleteEmployer(caseId.toString(), defendantId);

        employerHelper.getEmployer(defendantId, isJson(withJsonPath("$.size()", is(0))));

        assertThat(employerHelper.getEventFromPublicTopic(), employerHelper.getEmployerDeletedPublicEventMatcher(defendantId));
    }

    @Test
    public void shouldReturnEmptyObjectWhenEmployerDoNotExist() {
        final UUID nonExistingDefendantId = randomUUID();
        final Response response = employerHelper.getEmployer(nonExistingDefendantId.toString());
        assertThat(response.readEntity(String.class), is("{}"));
    }

    // return new employer with random name and address line 1
    private JsonObject getEmployerPayload() {
        final JsonObject address = createObjectBuilder()
                .add(FIELD_ADDRESS_1, RandomStringUtils.randomAlphabetic(12))
                .add(FIELD_ADDRESS_2, "Flat 8")
                .add(FIELD_ADDRESS_3, "Lant House")
                .add(FIELD_ADDRESS_4, "London")
                .add(FIELD_ADDRESS_5, "Greater London")
                .add(FIELD_POST_CODE, "SE1 1PJ").build();
        return createObjectBuilder()
                .add(FIELD_NAME, RandomStringUtils.randomAlphabetic(12))
                .add(FIELD_EMPLOYEE_REFERENCE, "abcdef")
                .add(FIELD_PHONE, "02020202020")
                .add(FIELD_ADDRESS, address).build();
    }

}
