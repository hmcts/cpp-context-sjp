package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.getEmployerDeletedPublicEventMatcher;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.getEmployerPayload;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.getEmployerUpdatedPayloadMatcher;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.getEmployerUpdatedPublicEventMatcher;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmployerIT extends BaseIntegrationTest {

    private EmployerHelper employerHelper;
    private FinancialMeansHelper financialMeansHelper;


    @Before
    public void setUp() {
        employerHelper = new EmployerHelper();
        financialMeansHelper = new FinancialMeansHelper();
    }

    @After
    public void tearDown() throws Exception {
        employerHelper.close();
        financialMeansHelper.close();
    }

    @Test
    public void shouldCreateUpdateAndDeleteEmployer() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");

        final JsonObject employer1 = getEmployerPayload();

        final JsonObject employer2 = getEmployerPayload();

        employerHelper.updateEmployer(caseId, defendantId, employer1);
        employerHelper.getEmployer(defendantId, getEmployerUpdatedPayloadMatcher(employer1));
        assertThat(employerHelper.getEventFromPublicTopic(), getEmployerUpdatedPublicEventMatcher(employer1));

        employerHelper.updateEmployer(caseId, defendantId, employer2);
        employerHelper.getEmployer(defendantId, getEmployerUpdatedPayloadMatcher(employer2));

        assertThat(employerHelper.getEventFromPublicTopic(), getEmployerUpdatedPublicEventMatcher(employer2));

        final Matcher<Object> expectedFinancialMeans = isJson(withJsonPath("$.employmentStatus", is("EMPLOYED")));
        financialMeansHelper.getFinancialMeans(defendantId, expectedFinancialMeans);

        employerHelper.deleteEmployer(caseId.toString(), defendantId);

        employerHelper.getEmployer(defendantId, isJson(withJsonPath("$.size()", is(0))));

        assertThat(employerHelper.getEventFromPublicTopic(), getEmployerDeletedPublicEventMatcher(defendantId));
    }

    @Test
    public void shouldReturnEmptyObjectWhenEmployerDoNotExist() {
        final UUID nonExistingDefendantId = randomUUID();
        final Response response = employerHelper.getEmployer(nonExistingDefendantId.toString());
        assertThat(response.readEntity(String.class), is("{}"));
    }


}
