package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.sjp.it.command.CreateCase;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class CaseListedInCriminalCourtsIT extends BaseIntegrationTest{

    private UUID caseId = UUID.randomUUID();

    @Before
    public void setUp() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);

        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

    @Test
    public void shouldUpdateCaseListedCriminalCourts() {

        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("progression.events.prosecutionCasesReferredToCourt", payload);
        }

        pollUntilCaseByIdIsOk(caseId, withJsonPath("$.listedInCriminalCourts", is(true)));
    }
}
