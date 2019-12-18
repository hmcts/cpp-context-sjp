package uk.gov.moj.cpp.sjp.event.processor;


import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OffenceWithdrawalRequestProcessorTest {

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> publicEventCaptor;

    @InjectMocks
    private OffenceWithdrawalRequestProcessor offenceWithdrawalRequestProcessor;

    @Test
    public void shouldHandleOffenceWithdrawalRelatedEvents() {
        assertThat(OffenceWithdrawalRequestProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleOffenceWithdrawalStatusSet").thatHandles("sjp.events.offences-withdrawal-status-set")));
    }

    @Test
    public void shouldEmitPublicEventWhenOffenceWithdrawalStatusSet() {
        final JsonObject privateEventPayload = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("setBy", randomUUID().toString())
                .add("setOn", now().toString())
                .add("withdrawalRequestsStatus", createArrayBuilder())
                .build();
        final JsonEnvelope privateEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.offences-withdrawal-status-set"), privateEventPayload);

        offenceWithdrawalRequestProcessor.handleOffenceWithdrawalStatusSet(privateEvent);

        verify(sender).send(publicEventCaptor.capture());
        final JsonEnvelope publicEvent = publicEventCaptor.getValue();

        assertThat(publicEvent.metadata(), metadata().envelopedWith(privateEvent.metadata()).withName("public.sjp.offences-withdrawal-status-set"));
        assertThat(publicEvent.payload(), equalTo(privateEvent.payload()));
    }
}
