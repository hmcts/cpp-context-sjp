package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OffencesWithdrawalRequestHelper extends AbstractTestHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(OffencesWithdrawalRequestHelper.class);
    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.request-withdrawal-all-offences+json";
    private static final String CASE_ID = "caseId";

    private CaseSjpHelper caseSjpHelper;
    private String request;

    public OffencesWithdrawalRequestHelper(CaseSjpHelper caseSjpHelper, String privateEvent, String publicEvent) {
        this.caseSjpHelper = caseSjpHelper;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(privateEvent);
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicEvent);
    }

    public void requestWithdrawalForAllOffences(final UUID userId) {
        final String writeUrl = String.format("/cases/%s/offences-withdrawal-request", caseSjpHelper.getCaseId());
        final JSONObject jsonObject = new JSONObject("{}");
        request = jsonObject.toString();

        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId);

        final Response response = restClient.postCommand(getWriteUrl(writeUrl), WRITE_MEDIA_TYPE, request, map);
        assertThat(response.getStatus(), equalTo(Response.Status.ACCEPTED.getStatusCode()));
    }

    public void verifyCaseUpdateRejectedPrivateInActiveMQ(final String expectedReason) {
        verifyCaseUpdateRejectedInActiveMQ(privateEventsConsumer, expectedReason);
    }

    public void verifyCaseUpdateRejectedPublicInActiveMQ(final String expectedReason) {
        verifyCaseUpdateRejectedInActiveMQ(publicEventsConsumer, expectedReason);
    }

    public void verifyAllOffencesWithdrawalInPrivateActiveMQ() {
        verifyCaseUpdateRejectedInActiveMQ(privateEventsConsumer);
    }

    public void verifyAllOffencesWithdrawalRequestedInPublicActiveMQ() {
        verifyCaseUpdateRejectedInActiveMQ(publicEventsConsumer);
    }

    private void verifyCaseUpdateRejectedInActiveMQ(final MessageConsumer messageConsumer) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());
        final JsonPath messageInQueue = retrieveMessage(messageConsumer);
        assertThat(messageInQueue.get(CASE_ID), equalTo(caseSjpHelper.getCaseId()));
    }

    private void verifyCaseUpdateRejectedInActiveMQ(final MessageConsumer messageConsumer, final String expectedReason) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());
        final JsonPath messageInQueue = retrieveMessage(messageConsumer);
        assertThat(messageInQueue.get(CASE_ID), equalTo(caseSjpHelper.getCaseId()));
        assertThat(messageInQueue.get("reason"), equalTo(expectedReason));
    }

}
