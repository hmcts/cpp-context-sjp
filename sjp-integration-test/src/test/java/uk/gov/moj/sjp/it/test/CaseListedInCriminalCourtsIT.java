package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class CaseListedInCriminalCourtsIT extends BaseIntegrationTest {

    private UUID caseId = randomUUID();

    @Before
    public void setUp() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);

        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder))
                .popEvent(CaseReceived.EVENT_NAME);
    }

    @Test
    public void shouldUpdateCaseListedCriminalCourts() {

        final JsonObject payload = createObjectBuilder()
                .add("prosecutionCaseId", caseId.toString())
                .build();

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.progression.prosecution-cases-referred-to-court", payload);
        }

        pollUntilCaseByIdIsOk(caseId, withJsonPath("$.listedInCriminalCourts", is(true)));
    }
}
