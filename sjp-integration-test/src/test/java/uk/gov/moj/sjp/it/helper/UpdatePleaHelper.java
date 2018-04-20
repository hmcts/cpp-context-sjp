package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdatePleaHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePleaHelper.class);

    private static final String WRITE_URL_PATTERN = "/cases/%s/offences/%s/pleas";

    private MessageConsumer publicEventsConsumer;

    public UpdatePleaHelper() {
        this(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED);
    }

    public UpdatePleaHelper(String publicEvent) {
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicEvent);
    }

    private String writeUrl(final UUID caseId, final UUID offenceId) {
        if (caseId == null || offenceId == null) {
            throw new IllegalArgumentException(String.format("Both values are required. CaseId '%s'. OffenceId: '%s'", caseId, offenceId));
        }
        return String.format(WRITE_URL_PATTERN, caseId, offenceId);
    }

    private void requestHttpCallWithPayloadAndStatus(
            final UUID caseId, final UUID offenceId, final String payload, final Response.Status expectedStatus) {

        LOGGER.info("Request payload: {}", new JsonPath(payload).prettify());

        makePostCall(writeUrl(caseId, offenceId), "application/vnd.sjp.update-plea+json", payload, expectedStatus);
    }

    public void updatePleaAndExpectBadRequest(final UUID caseId, final UUID offenceId, final JsonObject payload) {
        requestHttpCallWithPayloadAndStatus(caseId, offenceId, payload.toString(), Response.Status.BAD_REQUEST);
    }

    public void updatePlea(final UUID caseId, final UUID offenceId, final JsonObject payload) {
        requestHttpCallWithPayloadAndStatus(caseId, offenceId, payload.toString(), Response.Status.ACCEPTED);
    }

    public void verifyInPublicTopic(final UUID caseId, final UUID offenceId, final String plea, final String denialReason) {
        assertEventData(caseId, offenceId, plea, denialReason);
    }

    private void assertEventData(final UUID caseId, final UUID offenceId, final String plea, final String denialReason) {
        final JsonPath message = retrieveMessage(publicEventsConsumer);
        assertThat(message.get("caseId"), equalTo(caseId.toString()));
        assertThat(message.get("offenceId"), equalTo(offenceId.toString()));
        assertThat(message.get("plea"), equalTo(plea));
        assertThat(message.get("denialReason"), equalTo(denialReason));
    }

    public void verifyPleaUpdated(final UUID caseId, final String plea, final String pleaMethod) {
        CasePoller.pollUntilCaseByIdIsOk(caseId,
                allOf(
                        withJsonPath("defendant.offences[0].plea", is(plea)),
                        withJsonPath("defendant.offences[0].pleaMethod", is(pleaMethod))
                )
        );
    }

    public void verifyInterpreterLanguage(final UUID caseId, final String interpreterLanguage) {
        CasePoller.pollUntilCaseByIdIsOk(caseId,
                withJsonPath("defendant.interpreter.language",
                        is(interpreterLanguage))
        );
    }

    @Override
    public void close() {
        try {
            publicEventsConsumer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
