package uk.gov.moj.sjp.it.producer;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_COMPLETED;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;

public class CompleteCaseProducer {

    private static final String CASE_ID_PROPERTY = "caseId";

    private UUID caseId;
    private MessageConsumer privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_CASE_COMPLETED);

    public CompleteCaseProducer(UUID caseId) {
        this.caseId = caseId;
    }

    public void verifyInActiveMQ() {
        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get(CASE_ID_PROPERTY), equalTo(caseId.toString()));
    }

    public void completeCase() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("resultedOn", now().toString())
                .add("sjpSessionId", randomUUID().toString())
                .build();

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.resulting.referenced-decisions-saved", payload);
        }
    }

    public void assertCaseCompleted() {
        CasePoller.pollUntilCaseByIdIsOk(caseId, withJsonPath("$.completed", is(true)));
    }

}
