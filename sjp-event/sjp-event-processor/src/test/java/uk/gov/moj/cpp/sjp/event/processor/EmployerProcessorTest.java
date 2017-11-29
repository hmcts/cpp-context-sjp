package uk.gov.moj.cpp.sjp.event.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EmployerProcessorTest {

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private EmployerProcessor employerProcessor = new EmployerProcessor();

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldPublishPublicEventForEmployerUpdate() {
        final UUID defendantId = UUID.randomUUID();
        final JsonEnvelope privateEvent = envelope().with(metadataWithRandomUUID("sjp.events.employer-updated"))
                .withPayloadOf(defendantId.toString(), "defendantId")
                .build();

        employerProcessor.updateEmployer(privateEvent);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(), withMetadataEnvelopedFrom(privateEvent).withName("public.structure.employer-updated"));
        assertThat(publicEvent.payloadAsJsonObject(), equalTo(privateEvent.payloadAsJsonObject()));
    }

    @Test
    public void shouldPublishPublicEventForEmployerDeleted() {
        final UUID defendantId = UUID.randomUUID();
        final JsonEnvelope privateEvent = envelope().with(metadataWithRandomUUID("sjp.events.employer-deleted"))
                .withPayloadOf(defendantId.toString(), "defendantId")
                .build();

        employerProcessor.deleteEmployer(privateEvent);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(), withMetadataEnvelopedFrom(privateEvent).withName("public.structure.employer-deleted"));
        assertThat(publicEvent.payloadAsJsonObject(), equalTo(privateEvent.payloadAsJsonObject()));
    }
}
