package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OffencesWithdrawalRequestHelper implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OffencesWithdrawalRequestHelper.class);
    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.request-withdrawal-all-offences+json";
    private static final String CASE_ID = "caseId";

    private UUID caseId;
    private String request;
    private MessageConsumerClient publicConsumer = new MessageConsumerClient();
    private MessageConsumer privateEventsConsumer;
    private MessageConsumer publicEventsConsumer;

    public OffencesWithdrawalRequestHelper(final UUID caseId) {
        this(caseId, SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);
    }

    public OffencesWithdrawalRequestHelper(UUID caseId, String privateEvent, String publicEvent) {
        this.caseId = caseId;
        privateEventsConsumer = TopicUtil.privateEvents.createConsumer(privateEvent);
        publicEventsConsumer = TopicUtil.publicEvents.createConsumer(publicEvent);
    }

    public void requestWithdrawalForAllOffences(final UUID userId) {
        final String writeUrl = String.format("/cases/%s/offences-withdrawal-request", caseId);
        final JSONObject jsonObject = new JSONObject("{}");
        request = jsonObject.toString();

        makePostCall(userId, writeUrl, WRITE_MEDIA_TYPE, "{}", Response.Status.ACCEPTED);
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
        assertThat(messageInQueue, notNullValue());
        assertThat(messageInQueue.get(CASE_ID), equalTo(caseId.toString()));
    }

    private void verifyCaseUpdateRejectedInActiveMQ(final MessageConsumer messageConsumer, final String expectedReason) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());
        final JsonPath messageInQueue = retrieveMessage(messageConsumer);
        assertThat(messageInQueue, notNullValue());
        assertThat(messageInQueue.get(CASE_ID), equalTo(caseId.toString()));
        assertThat(messageInQueue.get("reason"), equalTo(expectedReason));
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
