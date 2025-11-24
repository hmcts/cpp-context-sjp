package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantPendingChangesAcceptedProcessorTest {
    @InjectMocks
    private DefendantPendingChangesAcceptedProcessor processor;
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldRaisePublicDefendantPendingChangesAcceptedEvent() {
        // given
        JsonObject eventPayload = createObjectBuilder().add("caseId", randomUUID().toString()).build();
        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.defendant-pending-changes-accepted"),
                eventPayload);

        // when
        processor.publish(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.sjp.events.defendant-pending-changes-accepted"));
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject(), is(eventPayload));
    }
}