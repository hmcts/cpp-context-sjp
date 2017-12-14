package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_REMOVE_PERSON_INFO;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.QueueUtil.privateEvents;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for DuplicatePersonInfo.
 */
public class RemovePersonInfoHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemovePersonInfoHelper.class);

    private static final String WRITE_MEDIA_TYPE_REMOVE_PERSON_INFO_PAYLOAD = "application/vnd.sjp.remove-person-info+json";
    private static final String TEMPLATE_REMOVE_PERSON_INFO_PAYLOAD = "raml/json/sjp.remove-person-info.json";

    private RestClient restClient = new RestClient();
    private CaseSjpHelper caseSjpHelper;

    private String request;
    private String personInfoId;

    public RemovePersonInfoHelper(final CaseSjpHelper caseSjpHelper, final String personInfoId) {
        this.personInfoId = personInfoId;
        this.caseSjpHelper = caseSjpHelper;
        privateEventsConsumer = privateEvents.createConsumer(EVENT_SELECTOR_REMOVE_PERSON_INFO);

    }

    public void addRemovePersonInfo() {
        final String writeUrl = String.format("/cases/%s/person-info/%s", caseSjpHelper.getCaseId(), personInfoId);
        final String payload = getPayload(TEMPLATE_REMOVE_PERSON_INFO_PAYLOAD);
        final JSONObject jsonObject = payloadAsJsonObject(payload);

        LOGGER.info("Remove Person Info action item with caseId {}", caseSjpHelper.getCaseId(), caseSjpHelper.getDefendantPersonId());
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HeaderConstants.USER_ID, USER_ID);
        request = jsonObject.toString();

        restClient.postCommand(getWriteUrl(writeUrl), WRITE_MEDIA_TYPE_REMOVE_PERSON_INFO_PAYLOAD, request, headers);
    }

    private JSONObject payloadAsJsonObject(final String payload) {
        JSONObject jsonObject = new JSONObject(payload);
        jsonObject.put("personId", caseSjpHelper.getDefendantPersonId());
        jsonObject.put("personInfoId",personInfoId);
        jsonObject.put("caseId",caseSjpHelper.getCaseId());
        return jsonObject;
    }

    public void verifyInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);

        assertThat(jsonResponse.get("personInfoId"), CoreMatchers.is(jsRequest.getString("personInfoId")));
        assertThat(jsonResponse.get("personId"), CoreMatchers.is(jsRequest.getString("personId")));
        assertThat(jsonResponse.get("caseId"), CoreMatchers.is(jsRequest.getString("caseId")));
    }

    public void verifyCaseSearchResultsCount(final int expectedCount) {
        try (final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseSjpHelper)) {
            caseSearchResultHelper.verifyPersonInfoByUrn(expectedCount);
        }
    }
}
