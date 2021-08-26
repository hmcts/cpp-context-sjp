package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.sjp.event.processor.matcher.SystemDocGeneratorEnvelopeMatchers.endorsementRemovalNotificationGeneratedCommand;
import static uk.gov.moj.cpp.sjp.event.processor.matcher.SystemDocGeneratorEnvelopeMatchers.endorsementRemovalNotificationGenerationFailedCommand;
import static uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier.NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.DocumentAvailableEventEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.GenerationFailedEventEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.SystemDocGeneratorEnvelopes;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EndorsementRemovalNotificationStrategyTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private EndorsementRemovalNotificationStrategy strategy;

    @Test
    public void shouldBeAbleToProcessKnownTemplateIdentifier() {
        final JsonEnvelope envelope = SystemDocGeneratorEnvelopes.documentAvailablePublicEvent()
                .templateIdentifier(NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue())
                .envelope();

        assertThat(strategy.canProcess(envelope), is(true));
    }

    @Test
    public void shouldNotBeAbleToProcessUnknownTemplateIdentifier() {
        final JsonEnvelope envelope = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier("Unknown")
                .envelope();

        assertThat(strategy.canProcess(envelope), is(false));
    }

    @Test
    public void shouldProcessDocumentAvailableEvent() {
        final UUID applicationDecisionId = randomUUID();
        final DocumentAvailableEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.documentAvailablePublicEvent()
                .templateIdentifier(NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue())
                .sourceCorrelationId(applicationDecisionId);

        strategy.process(envelopeBuilder.envelope());

        verify(sender).send(endorsementRemovalNotificationGeneratedCommand(applicationDecisionId,
                envelopeBuilder.getDocumentFileServiceId()));
    }

    @Test
    public void shouldProcessGenerationFailedEvent() {
        final UUID applicationDecisionId = randomUUID();
        final GenerationFailedEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier(NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue())
                .sourceCorrelationId(applicationDecisionId.toString());

        strategy.process(envelopeBuilder.envelope());

        verify(sender).send(endorsementRemovalNotificationGenerationFailedCommand(applicationDecisionId));
    }
}