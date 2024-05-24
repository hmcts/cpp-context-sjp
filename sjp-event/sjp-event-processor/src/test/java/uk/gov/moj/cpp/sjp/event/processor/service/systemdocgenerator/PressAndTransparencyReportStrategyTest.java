package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.DocumentAvailableEventEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.GenerationFailedEventEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.SystemDocGeneratorEnvelopes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PressAndTransparencyReportStrategyTest {

    @InjectMocks
    private PressAndTransparencyReportStrategy strategy;

    @Test
    public void shouldBeAbleToProcessSystemDocDocumentAvailablePublicEvent() {
        final DocumentAvailableEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.documentAvailablePublicEvent();

        final JsonEnvelope envelopeEnglish = envelopeBuilder.templateIdentifier("PendingCasesEnglish").envelope();
        assertThat(strategy.canProcess(envelopeEnglish), is(true));

        final JsonEnvelope envelopeWelsh = envelopeBuilder.templateIdentifier("PendingCasesWelsh").envelope();
        assertThat(strategy.canProcess(envelopeWelsh), is(true));

        final JsonEnvelope envelopePress = envelopeBuilder.templateIdentifier("PressPendingCasesFullEnglish").envelope();
        assertThat(strategy.canProcess(envelopePress), is(true));
    }

    @Test
    public void shouldBeAbleToProcessSystemDocGenerationFailedPublicEvent() {
        final GenerationFailedEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.generationFailedEvent();

        final JsonEnvelope envelopeEnglish = envelopeBuilder.templateIdentifier("PendingCasesEnglish").envelope();
        assertThat(strategy.canProcess(envelopeEnglish), is(true));

        final JsonEnvelope envelopeWelsh = envelopeBuilder.templateIdentifier("PendingCasesWelsh").envelope();
        assertThat(strategy.canProcess(envelopeWelsh), is(true));

        final JsonEnvelope envelopePress = envelopeBuilder.templateIdentifier("PressPendingCasesFullEnglish").envelope();
        assertThat(strategy.canProcess(envelopePress), is(true));
    }

    @Test
    public void shouldNotProcessUnknownTemplateIdentifier() {
        final JsonEnvelope generationFailedEnvelope = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier("random").envelope();
        final JsonEnvelope documentAvailableEnvelope = SystemDocGeneratorEnvelopes.documentAvailablePublicEvent()
                .templateIdentifier("random").envelope();

        assertThat(strategy.canProcess(generationFailedEnvelope), is(false));
        assertThat(strategy.canProcess(documentAvailableEnvelope), is(false));
    }
}