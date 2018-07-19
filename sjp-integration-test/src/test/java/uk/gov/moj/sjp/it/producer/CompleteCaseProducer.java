package uk.gov.moj.sjp.it.producer;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class CompleteCaseProducer {

    private UUID caseId;

    public CompleteCaseProducer(final UUID caseId) {
        this.caseId = caseId;
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
        pollUntilCaseByIdIsOk(caseId, withJsonPath("$.completed", is(true)));
    }

}
