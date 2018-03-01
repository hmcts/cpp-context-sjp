package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.http.RestPoller;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.util.QueueUtil;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdatePleaHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePleaHelper.class);

    private CaseSjpHelper caseSjpHelper;
    private String offenceId;
    private final String writeUrl;
    private MessageConsumerClient publicConsumer = new MessageConsumerClient();
    private MessageConsumer publicEventsConsumer;

    public UpdatePleaHelper(CaseSjpHelper caseSjpHelper) {
        this(caseSjpHelper, EVENT_SELECTOR_PLEA_UPDATED, PUBLIC_EVENT_SELECTOR_PLEA_UPDATED);
    }

    public UpdatePleaHelper(CaseSjpHelper caseSjpHelper, String privateEvent, String publicEvent) {
        this.caseSjpHelper = caseSjpHelper;
        this.offenceId = caseSjpHelper.getSingleOffenceId();
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicEvent);
        writeUrl = String.format("/cases/%s/offences/%s/pleas", caseSjpHelper.getCaseId(), offenceId);
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
        assertThat(message.get("caseId"), equalTo(caseSjpHelper.caseId));
        assertThat(message.get("offenceId"), equalTo(offenceId));
        assertThat(message.get("plea"), equalTo(plea));
        assertThat(message.get("denialReason"), equalTo(denialReason));
    }

    public void verifyPleaUpdated(final String plea, final String pleaMethod) {
        RestPoller.poll(getCaseById(caseSjpHelper.getCaseId()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("defendant.offences[0].plea", is(plea)),
                                withJsonPath("defendant.offences[0].pleaMethod", is(pleaMethod))
                        ))
                );
    }

    public void verifyInterpreterLanguage(final String interpreterLanguage) {
        RestPoller.poll(getCaseById(caseSjpHelper.getCaseId())).until(
                status().is(OK),
                payload().isJson(withJsonPath("defendant.interpreter.language",
                        is(interpreterLanguage)))
        );
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
