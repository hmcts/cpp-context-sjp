package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OffencesWithdrawalRequestCancelHelper implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OffencesWithdrawalRequestCancelHelper.class);
    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.cancel-request-withdrawal-all-offences+json";
    private static final String CASE_ID = "caseId";

    private UUID caseId;
    private String request;
    private MessageConsumerClient publicConsumer = new MessageConsumerClient();
    private MessageConsumer privateEventsConsumer;
    private MessageConsumer publicEventsConsumer;

    public OffencesWithdrawalRequestCancelHelper(final UUID caseId) {
        this(caseId, SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);
    }

    public OffencesWithdrawalRequestCancelHelper(final UUID caseId, final String privateEvent, final String publicEvent) {
        this.caseId = caseId;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(privateEvent);
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicEvent);
    }

    public void cancelRequestWithdrawalForAllOffences(final UUID userId) {
        final String writeUrl = String.format("/cases/%s/offences-withdrawal-request", caseId);
        final JSONObject jsonObject = new JSONObject("{}");
        request = jsonObject.toString();

        HttpClientUtil.makePostCall(userId, writeUrl, WRITE_MEDIA_TYPE, request, Response.Status.ACCEPTED);
    }

    public void verifyAllOffencesWithdrawalCancelledInPrivateActiveMQ() {
        verifyAllOffencesWithdrawalRequestCancelledInActiveMQ(privateEventsConsumer);
    }

    public void verifyAllOffencesWithdrawalRequestCancelledInPublicActiveMQ() {
        verifyAllOffencesWithdrawalRequestCancelledInActiveMQ(publicEventsConsumer);
    }

    private void verifyAllOffencesWithdrawalRequestCancelledInActiveMQ(final MessageConsumer messageConsumer) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());
        final JsonPath messageInQueue = retrieveMessage(messageConsumer);
        assertThat(messageInQueue.get(CASE_ID), equalTo(caseId.toString()));
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
