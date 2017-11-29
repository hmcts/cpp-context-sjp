package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.compile;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_DEFENDANT_ADDED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_DEFENDANT_ADDED;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDefendantSjpHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddDefendantHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.command.add-defendant+json";

    private static final String TEMPLATE_ADD_DEFENDANT_PAYLOAD = "raml/json/sjp.command.add-defendant.json";

    private final String caseId;
    private String request;

    private final String defendantId = UUID.randomUUID().toString();

    private final String personId = UUID.randomUUID().toString();

    public AddDefendantSjpHelper(String caseId) {
        this.caseId = caseId;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_DEFENDANT_ADDED);
        publicConsumer.startConsumer(PUBLIC_EVENT_SELECTOR_DEFENDANT_ADDED, PUBLIC_ACTIVE_MQ_TOPIC);
    }

    public void addDefendant() {
        final String jsonString = getPayload(TEMPLATE_ADD_DEFENDANT_PAYLOAD);
        JSONObject jsonObject = new JSONObject(jsonString);
        jsonObject.put("defendantId", defendantId);
        jsonObject.put("personId", personId);
        jsonObject.put("version", 1);
        jsonObject.getJSONArray("offences").getJSONObject(0).put("id", UUID.randomUUID().toString());
        request = jsonObject.toString();

        makePostCall(getWriteUrl("/cases/" + caseId + "/defendant"), WRITE_MEDIA_TYPE, request);
    }


    /**
     * Retrieve message from queue and do additional verifications
     */
    public void verifyInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);

        assertThat(jsonResponse.get("defendantId"), is(jsRequest.getString("defendantId")));
        assertThat(jsonResponse.get("personId"), is(jsRequest.getString("personId")));
        assertThat(jsonResponse.get("policeDefendantId"), is(jsRequest.getString("policeDefendantId")));
        assertThat(jsonResponse.get("nextHearing.courtID"), is(jsRequest.getString("nextHearing.courtID")));
        assertThat(jsonResponse.get("offences[0].policeOffenceId"), is(jsRequest.getString("offences[0].policeOffenceId")));
    }

    public void verifyInPublicTopic() {
        final String defendantAddedEvent = publicConsumer.retrieveMessage().orElse(null);

        assertThat(defendantAddedEvent, notNullValue());

        with(defendantAddedEvent)
                .assertThat("$.caseId", notNullValue())
                .assertThat("$.defendantId", notNullValue());
    }

    public void verifyDefendantAdded() {
        JsonPath jsRequest = new JsonPath(request);

        Filter personIdFilter = filter(where("personId").is(jsRequest.get("personId")));
        ResponseData response = poll(getCaseById(caseId))
                .until(
                        status().is(OK),
                        payload()
                                .isJson(allOf(
                                        withJsonPath(compile("$.defendants[?]", personIdFilter), hasSize(1))
                                )));

        assertThat(response.getPayload(), isJson(allOf(
                withJsonPath(compile("$.defendants[?].offences[0].id", personIdFilter), contains(jsRequest.getString("offences[0].id")))
        )));
    }

    public String getPersonId() {
        return personId;
    }

    public String getDefendantId() {
        return defendantId;
    }

}
