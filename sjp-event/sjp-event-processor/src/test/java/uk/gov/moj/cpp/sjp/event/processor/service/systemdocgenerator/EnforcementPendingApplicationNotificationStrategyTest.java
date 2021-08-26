package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier.ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.DocumentAvailableEventEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.GenerationFailedEventEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.SystemDocGeneratorEnvelopes;

import java.util.UUID;

import javax.json.JsonValue;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnforcementPendingApplicationNotificationStrategyTest {

    private static final String GENERATE_NOTIFICATION_COMMAND = "sjp.command.enforcement-pending-application-generate-notification";
    private static final String FAIL_GENERATION_COMMAND = "sjp.command.enforcement-pending-application-fail-generation-notification";

    @Mock
    private Sender sender;

    @InjectMocks
    private EnforcementPendingApplicationNotificationStrategy strategy;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Test
    public void shouldBeAbleToProcessKnownTemplateIdentifier() {
        final JsonEnvelope envelope = SystemDocGeneratorEnvelopes.documentAvailablePublicEvent()
                .templateIdentifier(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.getValue())
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
        final UUID applicationId = randomUUID();
        final DocumentAvailableEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.documentAvailablePublicEvent()
                .templateIdentifier(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.getValue())
                .sourceCorrelationId(applicationId);

        strategy.process(envelopeBuilder.envelope());

        verify(sender).send(jsonEnvelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = jsonEnvelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), is(GENERATE_NOTIFICATION_COMMAND));

        assertThat(sentEnvelope.payload(), payloadIsJson(allOf(
                withJsonPath("applicationId", Is.is(applicationId.toString())),
                withJsonPath("fileId", Is.is(envelopeBuilder.getDocumentFileServiceId().toString()))
        )));
    }

    @Test
    public void shouldProcessGenerationFailedEvent() {
        final UUID applicationId = randomUUID();
        final GenerationFailedEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.getValue())
                .sourceCorrelationId(applicationId.toString());

        final JsonEnvelope privateEvent = envelopeBuilder.envelope();
        strategy.process(privateEvent);

        verify(sender).send(jsonEnvelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = jsonEnvelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), is(FAIL_GENERATION_COMMAND));
        assertThat(sentEnvelope.payload(), payloadIsJson(allOf(
                withJsonPath("applicationId", Is.is(applicationId.toString())))));
    }
}