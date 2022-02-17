package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.sjp.event.processor.ConvictionCourtResolvedProcessor.PUBLIC_CONVICTION_COURT_RESOLVED;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.decision.ConvictionCourtResolved;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.ApplicationDecisionSetAsideEnvelope;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConvictionCourtResolvedProcessorTest {

    @Mock
    protected Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @InjectMocks
    private ConvictionCourtResolvedProcessor processor;

    private JsonEnvelope envelope;

    @Test
    public void shouldHandleConvictingCourtResolvedPublicEvent() {
        assertThat(ConvictionCourtResolvedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleResolveConvictionCourt").thatHandles(ConvictionCourtResolved.EVENT_NAME)));
    }

    @Test
    public void shouldPublishPublicEventOnConvictionCourtResolved() {
        final UUID caseId = randomUUID();
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();
        envelope = ApplicationDecisionSetAsideEnvelope.of(payload);
        processor.handleResolveConvictionCourt(envelope);
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PUBLIC_CONVICTION_COURT_RESOLVED));
        assertThat(envelopeCaptor.getValue().payload(), is(payload));
    }
}
