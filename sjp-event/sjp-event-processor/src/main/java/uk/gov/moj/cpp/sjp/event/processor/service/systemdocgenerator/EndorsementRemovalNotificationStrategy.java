package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier.NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

public class EndorsementRemovalNotificationStrategy implements SystemDocGeneratorResponseStrategy {

    private static final String NOTIFICATION_GENERATED_COMMAND = "sjp.command.endorsement-removal-notification-generated";
    private static final String GENERATION_FAILED_COMMAND = "sjp.command.endorsement-removal-notification-generation-failed";

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
        final String applicationDecisionId = getSourceCorrelationId(envelope);
        final String fileId = getDocumentFileServiceId(envelope);

        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(NOTIFICATION_GENERATED_COMMAND),
                createObjectBuilder()
                        .add("applicationDecisionId", applicationDecisionId)
                        .add("fileId", fileId)
                        .build());

        sender.send(envelopeToSend);
    }

    private void processGenerationFailed(final JsonEnvelope envelope) {
        final String applicationDecisionId = getSourceCorrelationId(envelope);

        final JsonEnvelope commandEnvelope = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(GENERATION_FAILED_COMMAND),
                createObjectBuilder()
                        .add("applicationDecisionId", applicationDecisionId)
                        .build());

        sender.send(commandEnvelope);
    }

    @Override
    public boolean canProcess(final JsonEnvelope envelope) {
        final String templateIdentifier = getTemplateIdentifier(envelope);
        return NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue().equals(templateIdentifier);
    }

    private String getDocumentFileServiceId(final JsonEnvelope envelope) {
        return envelope.payloadAsJsonObject().getString("documentFileServiceId");
    }
}
