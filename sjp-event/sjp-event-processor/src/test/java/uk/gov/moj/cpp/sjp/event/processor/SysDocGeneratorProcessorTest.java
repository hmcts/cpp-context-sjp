package uk.gov.moj.cpp.sjp.event.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.moj.cpp.sjp.event.processor.matcher.PressTransparencyReportEnvelopeMatchers;
import uk.gov.moj.cpp.sjp.event.processor.matcher.TransparencyReportEnvelopeMatchers;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.GenerationFailedEventEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.SystemDocGeneratorEnvelopes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SysDocGeneratorProcessorTest {

    private static final String TRANSPARENCY_TEMPLATE_IDENTIFIER_ENGLISH = "PendingCasesEnglish";
    private static final String TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH = "PendingCasesWelsh";
    private static final String PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER = "PressPendingCasesEnglish";

    @Mock
    private Sender sender;
    @InjectMocks
    private SysDocGeneratorProcessor processor;

    @Test
    public void shouldSendTransparencyReportCommandBasedOnTemplateName() {
        final GenerationFailedEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier(TRANSPARENCY_TEMPLATE_IDENTIFIER_ENGLISH);

        processor.handleDocumentGenerationFailedEvent(envelopeBuilder.envelope());

        final String transparencyReportId = envelopeBuilder.getSourceCorrelationId();
        verify(sender).send(TransparencyReportEnvelopeMatchers.reportFailedCommand(transparencyReportId));
    }

    @Test
    public void shouldSendTransparencyReportCommandBasedOnTemplateNameInWelsh() {
        final GenerationFailedEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier(TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH);

        processor.handleDocumentGenerationFailedEvent(envelopeBuilder.envelope());

        final String transparencyReportId = envelopeBuilder.getSourceCorrelationId();
        verify(sender).send(TransparencyReportEnvelopeMatchers.reportFailedCommand(transparencyReportId));
    }

    @Test
    public void shouldSendPressTransparencyReportCommandBasedOnTemplateName() {
        final GenerationFailedEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier(PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER);

        processor.handleDocumentGenerationFailedEvent(envelopeBuilder.envelope());

        final String pressTransparencyReportId = envelopeBuilder.getSourceCorrelationId();
        verify(sender).send(PressTransparencyReportEnvelopeMatchers.reportFailedCommand(pressTransparencyReportId));
    }

    @Test
    public void shouldNotInvokeAnyCommandIfTheTemplateIdentifierIsUnknown() {
        final GenerationFailedEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier("");

        processor.handleDocumentGenerationFailedEvent(envelopeBuilder.envelope());

        verify(sender, never()).send(any());
    }

    @Test
    public void shouldNotInvokeAnyCommandIfTheTemplateIdentifierIsNotPresent() {
        final GenerationFailedEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier(null);

        processor.handleDocumentGenerationFailedEvent(envelopeBuilder.envelope());

        verify(sender, never()).send(any());
    }
}