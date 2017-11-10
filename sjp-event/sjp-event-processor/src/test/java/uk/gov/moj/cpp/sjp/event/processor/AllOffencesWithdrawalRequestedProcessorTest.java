package uk.gov.moj.cpp.sjp.event.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.AllOffencesWithdrawalRequestedProcessor;

import javax.json.Json;

import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

@RunWith(MockitoJUnitRunner.class)
public class AllOffencesWithdrawalRequestedProcessorTest {

    private static final String CASE_ID = UUID.randomUUID().toString();

    @InjectMocks
    private AllOffencesWithdrawalRequestedProcessor allOffencesWithdrawalRequestedProcessor;
    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;
    @Spy
    private Enveloper envelopers = createEnveloper();

    @Test
    public void publishAllOffencesWithdrawalRequestedPublicEvent() throws Exception {

        final JsonEnvelope privateEvent = createEnvelope("structure.events.all-offences-withdrawal-requested",
                Json.createObjectBuilder().add("caseId", CASE_ID).build());
        allOffencesWithdrawalRequestedProcessor.publishAllOffencesWithdrawalEvent(privateEvent);

        verify(sender).send(captor.capture());
        final JsonEnvelope publicEvent = captor.getValue();
        assertThat(publicEvent, jsonEnvelope(
                metadata().withName("public.structure.all-offences-withdrawal-requested"),
                payloadIsJson(withJsonPath("$.caseId", equalTo(CASE_ID)))
        ));
    }
}