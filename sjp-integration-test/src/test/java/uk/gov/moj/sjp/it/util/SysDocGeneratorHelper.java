package uk.gov.moj.sjp.it.util;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.JsonObject;

public class SysDocGeneratorHelper {

    private static final String CONVERSION_FORMAT = "PDF";
    private static final String ORIGINATING_SOURCE = "sjp";


    public static void publishDocumentAvailablePublicEvent(final UUID sourceCorrelationId,
                                                           final String templateIdentifier,
                                                           final UUID documentFileServiceId) {
        publishDocumentAvailablePublicEvent(sourceCorrelationId, templateIdentifier, randomUUID(), documentFileServiceId);
    }

    public static void publishDocumentAvailablePublicEvent(final UUID sourceCorrelationId,
                                                           final String templateIdentifier,
                                                           final UUID payloadFileServiceId,
                                                           final UUID documentFileServiceId) {
        final JsonObject payload = documentAvailablePayload(
                sourceCorrelationId,
                templateIdentifier,
                payloadFileServiceId,
                documentFileServiceId
        );

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer(PUBLIC_EVENT);
            producerClient.sendMessage("public.systemdocgenerator.events.document-available", payload);
        }
    }

    public static void publishGenerationFailedPublicEvent(final UUID sourceCorrelationId,
                                                          final String templateIdentifier) {
        publishGenerationFailedPublicEvent(sourceCorrelationId, templateIdentifier, randomUUID());
    }

    public static void publishGenerationFailedPublicEvent(final UUID sourceCorrelationId,
                                                          final String templateIdentifier,
                                                          final UUID payloadFileServiceId) {
        final JsonObject payload = documentFailurePayload(
                sourceCorrelationId,
                templateIdentifier,
                payloadFileServiceId
        );

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer(PUBLIC_EVENT);
            producerClient.sendMessage("public.systemdocgenerator.events.generation-failed", payload);
        }
    }

    private static JsonObject documentAvailablePayload(final UUID sourceCorrelationId,
                                                       final String templateIdentifier,
                                                       final UUID payloadFileServiceId,
                                                       final UUID documentFileServiceId) {
        return createObjectBuilder()
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("conversionFormat", CONVERSION_FORMAT)
                .add("originatingSource", ORIGINATING_SOURCE)
                .add("requestedTime", currentTimeMinus10Seconds())
                .add("generatedTime", currentTime())
                .add("generateVersion", 1)
                .build();
    }

    private static JsonObject documentFailurePayload(final UUID sourceCorrelationId,
                                                     final String templateIdentifier,
                                                     final UUID payloadFileServiceId) {
        return createObjectBuilder()
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("payloadFileServiceId", payloadFileServiceId.toString())
                .add("conversionFormat", CONVERSION_FORMAT)
                .add("originatingSource", ORIGINATING_SOURCE)
                .add("requestedTime", currentTimeMinus10Seconds())
                .add("failedTime", currentTime())
                .add("reason", "mock failure")
                .build();
    }

    private static String currentTime() {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
    }

    private static String currentTimeMinus10Seconds() {
        return ZonedDateTime.now().minusSeconds(10).format(DateTimeFormatter.ISO_INSTANT);
    }
}
