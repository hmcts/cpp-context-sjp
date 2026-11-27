package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier.ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnforcementPendingApplicationNotificationStrategy implements SystemDocGeneratorResponseStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnforcementPendingApplicationNotificationStrategy.class);

    private static final String GENERATE_NOTIFICATION_COMMAND = "sjp.command.enforcement-pending-application-generate-notification";
    private static final String FAIL_GENERATION_COMMAND = "sjp.command.enforcement-pending-application-fail-generation-notification";

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    @Override
    public void process(final JsonEnvelope envelope) {
        if (isDocumentAvailablePublicEvent(envelope)) {
            processDocumentAvailable(envelope);
        }
        if (isGenerationFailedPublicEvent(envelope)) {
            processGenerationFailed(envelope);
        }
    }

    private void processDocumentAvailable(final JsonEnvelope envelope) {
        final String applicationId = getSourceCorrelationId(envelope);
        final String fileId = getDocumentFileServiceId(envelope);
        final String generatedTime = envelope.payloadAsJsonObject().getString("generatedTime", "");
        LOGGER.info("enforcementpendingapplicationgeneratenotificationpayload:{}", envelope.payloadAsJsonObject());

        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(GENERATE_NOTIFICATION_COMMAND),
                createObjectBuilder()
                        .add("applicationId", applicationId)
                        .add("fileId", fileId)
                        .add("generatedTime", generatedTime)
                        .build());

        sender.send(envelopeToSend);
    }

    private void processGenerationFailed(final JsonEnvelope envelope) {
        final String applicationId = getSourceCorrelationId(envelope);
        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(FAIL_GENERATION_COMMAND),
                createObjectBuilder()
                        .add("applicationId", applicationId)
                        .add("generationFailedTime", ZonedDateTime.now().toString())
                        .build());

        sender.send(commandEnvelope);
    }

    @Override
    public boolean canProcess(final JsonEnvelope envelope) {
        final String templateIdentifier = getTemplateIdentifier(envelope);
        return ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.getValue().equals(templateIdentifier);
    }

    private String getDocumentFileServiceId(final JsonEnvelope envelope) {
        return envelope.payloadAsJsonObject().getString("documentFileServiceId");
    }
}
