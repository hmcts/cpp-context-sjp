package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.Json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

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

        final JsonEnvelope privateEvent = createEnvelope("sjp.events.all-offences-withdrawal-requested",
                Json.createObjectBuilder().add("caseId", CASE_ID).build());
        allOffencesWithdrawalRequestedProcessor.publishAllOffencesWithdrawalEvent(privateEvent);

        verify(sender).send(captor.capture());
        final JsonEnvelope publicEvent = captor.getValue();
        assertThat(publicEvent, jsonEnvelope(
                metadata().withName("public.sjp.all-offences-withdrawal-requested"),
                payloadIsJson(withJsonPath("$.caseId", equalTo(CASE_ID)))
        ));
    }
}