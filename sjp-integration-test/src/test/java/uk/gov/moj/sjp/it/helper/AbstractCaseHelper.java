package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;
import static uk.gov.moj.sjp.it.util.SchemaValidatorUtil.validateAgainstSchema;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;


/**
 * Helper to class to support Write / Read operations for assignment
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractCaseHelper extends AbstractTestHelper {

    public static final String GET_CASE_BY_ID_MEDIA_TYPE = "application/vnd.sjp.query.case+json";
    public static final String GET_CASE_BY_URN_MEDIA_TYPE = "application/vnd.sjp.query.case-by-urn+json";
    public static final String ASSOCIATE_ENTERPRISE_ID_CONTENT_TYPE = "application/vnd.enterprise-id+json";
    protected String caseId;
    protected String offenceId;
    protected String request;
    protected String caseUrn;
    protected JsonPath jsonResponse;

    public AbstractCaseHelper() {
        caseId = UUID.randomUUID().toString();
        offenceId = UUID.randomUUID().toString();
        caseUrn = RandomGenerator.integer(10, 99).next() + "GD" + RandomGenerator.integer(10000, 99999).next() + "16";
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(getEventSelector());
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(getPublicEventSelector());
    }

    protected abstract String getPublicEventSelector();

    public void createCase() {
        String payload = getPayloadForCreatingCase();
        createCase(payload);
    }

    public void createCaseWith(List<Map<String, String>> caseMarkers) {
        String payload = getPayloadForCreatingCaseWithCaseMarkers(caseMarkers);
        createCase(payload);
    }

    private void createCase(String payload) {
        makePostCall(getWriteUrl("/cases"), getWriteMediaType(), payload);
    }

    public void associateEnterpriseIdWIthCase() {
        final String payload = getPayloadForEnterpriseId();
        makePostCall(getWriteUrl("/cases/" + getCaseId()), ASSOCIATE_ENTERPRISE_ID_CONTENT_TYPE, payload);
    }

    /**
     * Retrieve message from queue and do additional verifications
     */
    public void verifyInPrivateActiveMQ() {
        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get("id"), is(caseId));
        assertThat(jsonResponse.get("urn"), is(caseUrn));
    }

    public void verifyCaseCreatedUsingId() {
        final ResponseData caseResponse = poll(getCaseById(caseId))
                .timeout(40, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.id", is(caseId))
                        )
                );

        jsonResponse = new JsonPath(caseResponse.getPayload());
        validateAgainstSchema("sjp.query.case.json", JSONObject.class, jsonResponse);

        JsonPath jsonRequest = new JsonPath(request);
        doAdditionalReadCallResponseVerification(jsonRequest, jsonResponse);

    }

    public void verifyCaseQueryReturnsNotAvailable() {
        poll(getCaseById(caseId))
                .until(status().is(FORBIDDEN)
                );
    }

    public void verifyCaseCreatedUsingUrn() {
        final ResponseData caseResponse = poll(getCaseById(caseId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.urn", is(caseUrn))
                        ))
                );

        jsonResponse = new JsonPath(caseResponse.getPayload());
        validateAgainstSchema("sjp.query.case.json", JSONObject.class, jsonResponse);

        JsonPath jsonRequest = new JsonPath(request);
        doAdditionalReadCallResponseVerification(jsonRequest, jsonResponse);
    }

    public void verifyEnterpriseIdAssociatedWithCase(final String enterpriseId) {
        final ResponseData caseResponse = poll(getCaseById(caseId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.urn", is(caseUrn)),
                                withJsonPath("$.enterpriseId", is(enterpriseId))
                        ))
                );

        jsonResponse = new JsonPath(caseResponse.getPayload());
        validateAgainstSchema("sjp.query.case.json", JSONObject.class, jsonResponse);

        JsonPath jsonRequest = new JsonPath(request);
        doAdditionalReadCallResponseVerification(jsonRequest, jsonResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assertQueryCallResponseStatusIs(Response.Status status) {
        assertQueryCallResponseStatusIs(status, USER_ID);
    }

    public void assertQueryCallResponseStatusIs(Response.Status status, String userId) {
        Response response = makeGetCall(getReadUrl("/cases/" + caseId), GET_CASE_BY_ID_MEDIA_TYPE, userId);
        assertThat("Response status code should be " + status, response.getStatus(), is(status.getStatusCode()));

        response = makeGetCall(getReadUrl("/cases?urn=" + caseUrn), GET_CASE_BY_URN_MEDIA_TYPE, userId);
        assertThat("Response status code should be " + status, response.getStatus(), is(status.getStatusCode()));
    }

    public int getCaseVersion() {
        final ResponseData caseResponse = poll(getCaseById(caseId))
                .until(
                        status().is(OK)
                );

        final JsonPath jsonResponse = new JsonPath(caseResponse.getPayload());
        return jsonResponse.getInt("version");
    }

    public String getCaseResponseDataUsingId() {
        final ResponseData caseResponse = poll(getCaseById(caseId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(caseId))
                        ))
                );

        return caseResponse.getPayload();
    }

    public JsonPath getCaseResponseUsingId() {
        return new JsonPath(getCaseResponseDataUsingId());
    }

    public void assertCaseDoesNotExist(String caseIdentifier) {
        poll(getCaseById(caseIdentifier))
                .until(
                        status().is(NOT_FOUND)
                );
    }

    /**
     * Utility method to abstract all operations around creating a case and verifying successful
     * creation
     */
    public void createAndVerifyCase() {
        createCase();
        verifyCaseCreatedUsingId();
    }

    protected String getPayloadForCreatingCase() {

        String templateRequest = getPayload(getTemplatePayloadPath());

        // updating case ID and URN to ensure uniqueness when test runs
        JSONObject jsonObject = new JSONObject(templateRequest);
        jsonObject.put("id", caseId);
        jsonObject.put("urn", caseUrn);
        doAdditionalReplacementOfValues(jsonObject);
        request = jsonObject.toString();
        return request;
    }

    protected String getPayloadForEnterpriseId() {
        final JsonObject payload = createObjectBuilder()
                .add("enterpriseId", "2K2SLYFC743H").build();

        return payload.toString();
    }

    protected String getPayloadForCreatingCaseWithCaseMarkers(List<Map<String, String>> caseMarkers) {
        String templateRequest = getPayload(getTemplatePayloadPath());

        // updating case ID and URN to ensure uniqueness when test runs
        JSONObject jsonObject = new JSONObject(templateRequest);
        jsonObject.put("id", caseId);
        jsonObject.put("urn", caseUrn);
        jsonObject.put("caseMarkers", caseMarkers);
        request = jsonObject.toString();
        return request;
    }

    protected abstract void doAdditionalReadCallResponseVerification(JsonPath jsonRequest, JsonPath jsonResponse);

    protected abstract String getTemplatePayloadPath();

    protected abstract String getWriteMediaType();

    protected abstract String getEventSelector();

    protected abstract void doAdditionalReplacementOfValues(JSONObject jsonObject);

    public String getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }
}
