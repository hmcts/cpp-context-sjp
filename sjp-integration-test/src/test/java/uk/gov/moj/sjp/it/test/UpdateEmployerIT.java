package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithDecision;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.EmployerHelper;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UpdateEmployerIT extends BaseIntegrationTest {

    private EmployerHelper employerHelper;
    private CaseSjpHelper caseSjpHelper;

    private static final String FIELD_NAME = "name";
    private static final String FIELD_EMPLOYEE_REFERENCE = "employeeReference";
    private static final String FIELD_PHONE = "phone";
    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_ADDRESS_1 = "address1";
    private static final String FIELD_ADDRESS_2 = "address2";
    private static final String FIELD_ADDRESS_3 = "address3";
    private static final String FIELD_ADDRESS_4 = "address4";
    private static final String FIELD_POST_CODE = "postCode";

    @Before
    public void setUp() {
        employerHelper = new EmployerHelper();
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.verifyCaseCreatedUsingId();
    }

    @After
    public void tearDown() throws Exception {
        caseSjpHelper.close();
        employerHelper.close();
    }

    @Test
    public void shouldCreateAndUpdateEmployer() {
        stubGetCaseDecisionsWithNoDecision(caseSjpHelper.getCaseId());
        stubGetEmptyAssignmentsByDomainObjectId(caseSjpHelper.getCaseId());

        final String defendantId = caseSjpHelper.getSingleDefendantId();
        final String caseId = caseSjpHelper.getCaseId();

        final JsonObject employer1 = getEmployerPayload();
        final Matcher expectedEmployerMatcher1 = getMatcher(employer1);

        final JsonObject employer2 = getEmployerPayload();
        final Matcher expectedEmployerMatcher2 = getMatcher(employer2);

        employerHelper.updateEmployer(caseId, defendantId, employer1);
        employerHelper.getEmployer(defendantId, expectedEmployerMatcher1);
        assertThat(employerHelper.getEventFromPublicTopic(), expectedEmployerMatcher1);

        employerHelper.updateEmployer(caseId, defendantId, employer2);
        employerHelper.getEmployer(defendantId, expectedEmployerMatcher2);
        assertThat(employerHelper.getEventFromPublicTopic(), expectedEmployerMatcher2);
    }

    @Test
    public void shouldReturnEmptyObjectWhenEmployerDoNotExist() {
        final UUID nonExistingDefendantId = randomUUID();
        final Response response = employerHelper.getEmployer(nonExistingDefendantId.toString());
        assertThat(response.readEntity(String.class), is("{}"));
    }

    @Test
    public void shouldRejectEmployerUpdateIfCaseIsAlreadyCompleted() throws Exception {

        stubGetCaseDecisionsWithDecision(caseSjpHelper.getCaseId());

        final String defendantId = caseSjpHelper.getSingleDefendantId();
        final String caseId = caseSjpHelper.getCaseId();

        final JsonObject employer = getEmployerPayload();

        final Matcher expectedCaseUpdateRejectedMatcher = isJson(allOf(
                withJsonPath("$.caseId", is(caseId)),
                withJsonPath("$.reason", is("CASE_COMPLETED"))
        ));

        employerHelper.updateEmployer(caseId, defendantId, employer);
        assertThat(employerHelper.getEventFromPublicTopic(), expectedCaseUpdateRejectedMatcher);
    }

    // return new employer with random name and address line 1
    private JsonObject getEmployerPayload() {
        final JsonObject address = createObjectBuilder()
                .add(FIELD_ADDRESS_1, RandomStringUtils.randomAlphabetic(12))
                .add(FIELD_ADDRESS_2, "suburb")
                .add(FIELD_ADDRESS_3, "city")
                .add(FIELD_ADDRESS_4, "county")
                .add(FIELD_POST_CODE, "AB3 4EF").build();
        return createObjectBuilder()
                .add(FIELD_NAME, RandomStringUtils.randomAlphabetic(12))
                .add(FIELD_EMPLOYEE_REFERENCE, "abcdef")
                .add(FIELD_PHONE, "02020202020")
                .add(FIELD_ADDRESS, address).build();
    }

    private Matcher getMatcher(final JsonObject employer) {
        final JsonObject address = employer.getJsonObject(FIELD_ADDRESS);
        return isJson(allOf(
                withJsonPath("$.name", equalTo(employer.getString(FIELD_NAME))),
                withJsonPath("$.employeeReference", equalTo(employer.getString(FIELD_EMPLOYEE_REFERENCE))),
                withJsonPath("$.phone", equalTo(employer.getString(FIELD_PHONE))),
                withJsonPath("$.address.address1", equalTo(address.getString(FIELD_ADDRESS_1))),
                withJsonPath("$.address.address2", equalTo(address.getString(FIELD_ADDRESS_2))),
                withJsonPath("$.address.address3", equalTo(address.getString(FIELD_ADDRESS_3))),
                withJsonPath("$.address.address4", equalTo(address.getString(FIELD_ADDRESS_4))),
                withJsonPath("$.address.postCode", equalTo(address.getString(FIELD_POST_CODE)))
        ));
    }
}
