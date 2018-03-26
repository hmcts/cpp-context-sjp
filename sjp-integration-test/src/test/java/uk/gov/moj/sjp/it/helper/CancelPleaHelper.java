package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;

public class CancelPleaHelper implements AutoCloseable {

    private UUID offenceId;
    private UUID caseId;
    private final String writeUrl;
    private MessageConsumerClient publicConsumer = new MessageConsumerClient();
    private MessageConsumer publicEventsConsumer;

    public CancelPleaHelper(final UUID caseId, final UUID offenceId) {
        this(caseId, offenceId, EVENT_SELECTOR_PLEA_CANCELLED, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED);
    }

    public CancelPleaHelper(final UUID caseId, final UUID offenceId, final String privateEvent, final String publicEvent) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicEvent);
        writeUrl = String.format("/cases/%s/offences/%s/pleas", caseId, offenceId);
    }

    public void cancelPlea() {
        final String contentType = "application/vnd.sjp.cancel-plea+json";
        final String payload = "{}";
        HttpClientUtil.makePostCall(writeUrl, contentType, payload);
    }

    public void verifyInPublicTopic() {
        assertEventData(publicEventsConsumer);
    }

    private void assertEventData(final MessageConsumer publicEventsConsumer) {
        final JsonPath message = retrieveMessage(publicEventsConsumer);
        assertThat(message.get("caseId"), equalTo(caseId.toString()));
        assertThat(message.get("offenceId"), equalTo(offenceId.toString()));
    }

    public void verifyPleaCancelled() {
        CasePoller.pollUntilCaseByIdIsOk(caseId,
                allOf(
                        withoutJsonPath("defendant.offences[0].plea"),
                        withoutJsonPath("defendant.offences[0].pleaMethod"))
        );
    }

    public void verifyInterpreterCancelled() {
        CasePoller.pollUntilCaseByIdIsOk(caseId,
                withoutJsonPath("defendants[0].interpreter.language")        
        );
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
