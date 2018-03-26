package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdatePleaHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePleaHelper.class);

    private UUID caseId;
    private UUID offenceId;
    private final String writeUrl;
    private MessageConsumerClient publicConsumer = new MessageConsumerClient();
    private MessageConsumer publicEventsConsumer;

    public UpdatePleaHelper(final UUID caseId, final UUID offenceId) {
        this(caseId, offenceId, EVENT_SELECTOR_PLEA_UPDATED, PUBLIC_EVENT_SELECTOR_PLEA_UPDATED);
    }

    public UpdatePleaHelper(final UUID caseId, final UUID offenceId, String privateEvent, String publicEvent) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicEvent);
        writeUrl = String.format("/cases/%s/offences/%s/pleas", caseId, offenceId);
    }

    private void requestHttpCallWithPayloadAndStatus(final String payload,
                                                     final Response.Status expectedStatus) {

        LOGGER.info("Request payload: {}", new JsonPath(payload).prettify());

        makePostCall(writeUrl, "application/vnd.sjp.update-plea+json", payload, expectedStatus);
    }

    public void updatePleaAndExpectBadRequest(final JsonObject payload) {
        requestHttpCallWithPayloadAndStatus(payload.toString(), Response.Status.BAD_REQUEST);
    }

    public void updatePlea(final JsonObject payload) {
        requestHttpCallWithPayloadAndStatus(payload.toString(), Response.Status.ACCEPTED);
    }

    public void verifyInPublicTopic(final String plea, final String denialReason) {
        assertEventData(publicEventsConsumer, plea, denialReason);
    }

    private void assertEventData(final MessageConsumer publicEventsConsumer, final String plea, final String denialReason) {
        final JsonPath message = retrieveMessage(publicEventsConsumer);
        assertThat(message.get("caseId"), equalTo(caseId.toString()));
        assertThat(message.get("offenceId"), equalTo(offenceId.toString()));
        assertThat(message.get("plea"), equalTo(plea));
        assertThat(message.get("denialReason"), equalTo(denialReason));
    }

    public void verifyPleaUpdated(final String plea, final String pleaMethod) {
        CasePoller.pollUntilCaseByIdIsOk(caseId,
                allOf(
                        withJsonPath("defendant.offences[0].plea", is(plea)),
                        withJsonPath("defendant.offences[0].pleaMethod", is(pleaMethod))
                )
        );
    }

    public void verifyInterpreterLanguage(final String interpreterLanguage) {
        CasePoller.pollUntilCaseByIdIsOk(caseId,
                withJsonPath("defendant.interpreter.language",
                        is(interpreterLanguage))
        );
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
