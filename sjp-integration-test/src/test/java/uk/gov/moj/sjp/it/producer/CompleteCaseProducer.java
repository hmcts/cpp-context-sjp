package uk.gov.moj.sjp.it.producer;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class CompleteCaseProducer {

    private final UUID caseId;

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
        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.completed", is(true)),
                withJsonPath("$.status", is(CaseStatus.COMPLETED.name())
                )));
    }

    public void completeCaseResults() {
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

    public void assertCaseResults() {
        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.completed", is(true)),
                withJsonPath("$.status", is(CaseStatus.COMPLETED.name())
                )));
    }


}
