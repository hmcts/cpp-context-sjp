package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

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
public class DefendantDetailsUpdateProcessorTest {

    @InjectMocks
    private DefendantDetailsUpdateProcessor processor;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void updatesPeopleWhenDefendandDetailsUpdated() {
        // given
        JsonObject eventPayload = createObjectBuilder().build();
        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.defendant-details-updated"),
                eventPayload);

        // when
        processor.publish(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.sjp.events.defendant-details-updated"));
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject(), is(eventPayload));
    }
}