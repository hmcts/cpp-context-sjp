package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_COMPLETED;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.sjp.it.util.QueueUtil;

import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;

/**
 * Helper class for Complete Case IT.
 */
public class CompleteCaseHelper extends AbstractTestHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.command.complete-case+json";

    private static final String CASE_ID_PROPERTY = "caseId";

    private AbstractCaseHelper caseHelper;

    public CompleteCaseHelper(AbstractCaseHelper caseHelper) {
        this.caseHelper = caseHelper;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_CASE_COMPLETED);
    }

    public void verifyInActiveMQ() {
        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get(CASE_ID_PROPERTY), equalTo(caseHelper.getCaseId()));
    }

    public void completeCase() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseHelper.getCaseId())
                .add("resultedOn", now().toString())
                .add("sjpSessionId", randomUUID().toString())
                .build();

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.resulting.referenced-decisions-saved", payload);
        }
    }

    public void assertCaseCompleted() {
        poll(getCaseById(caseHelper.getCaseId()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.completed", is(true))
                        ))
                );
    }
}
