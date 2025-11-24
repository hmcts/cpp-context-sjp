package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import uk.gov.justice.services.messaging.JsonEnvelope;

public interface SystemDocGeneratorResponseStrategy {

    boolean canProcess(final JsonEnvelope envelope);

    void process(final JsonEnvelope envelope);

    default String getTemplateIdentifier(final JsonEnvelope envelope) {
        return envelope.payloadAsJsonObject().getString("templateIdentifier", "");
    }

    default String getSourceCorrelationId(final JsonEnvelope envelope) {
        return envelope.payloadAsJsonObject().getString("sourceCorrelationId", "");
    }

    default boolean isDocumentAvailablePublicEvent(final JsonEnvelope envelope) {
        final String eventName = envelope.metadata().name();
        return "public.systemdocgenerator.events.document-available".equals(eventName);
    }

    default boolean isGenerationFailedPublicEvent(final JsonEnvelope envelope) {
        final String eventName = envelope.metadata().name();
        return "public.systemdocgenerator.events.generation-failed".equals(eventName);
    }
}
