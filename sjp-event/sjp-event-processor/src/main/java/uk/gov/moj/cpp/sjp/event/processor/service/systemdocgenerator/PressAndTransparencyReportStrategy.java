package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.utils.PdfHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PressAndTransparencyReportStrategy implements SystemDocGeneratorResponseStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressAndTransparencyReportStrategy.class);

    private static final String TRANSPARENCY_TEMPLATE_IDENTIFIER = "PublicPendingCasesFullEnglish";
    private static final String TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA = "PublicPendingCasesDeltaEnglish";
    private static final String TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH = "PublicPendingCasesFullWelsh";
    private static final String TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA = "PublicPendingCasesDeltaWelsh";
    private static final String PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER = "PressPendingCasesFullEnglish";
    private static final String PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA = "PressPendingCasesDeltaEnglish";
    private static final String PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH = "PressPendingCasesFullWelsh";
    private static final String PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA = "PressPendingCasesDeltaWelsh";
    private static final String COMMAND_INGEST_FILE = "sjp.ingest-file";
    private static final Map<String, String> TEMPLATE_TO_FILENAME = Map.of(
            TRANSPARENCY_TEMPLATE_IDENTIFIER,       "transparency-report-full-english.pdf",
            TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA, "transparency-report-delta-english.pdf",
            TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH, "transparency-report-full-welsh.pdf",
            TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA, "transparency-report-delta-welsh.pdf",
            PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER,       "press-transparency-report-full-english.pdf",
            PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA, "press-transparency-report-delta-english.pdf",
            PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH, "press-transparency-report-full-welsh.pdf",
            PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA, "press-transparency-report-delta-welsh.pdf"
    );
    private static final String COMMAND_TRANSPARENCY_REPORT_FAILED = "sjp.command.transparency-report-failed";
    private static final String COMMAND_PRESS_TRANSPARENCY_REPORT_FAILED = "sjp.command.press-transparency-report-failed";
    private static final Locale ENGLISH = new Locale("en", "GB");
    private static final Locale WELSH = new Locale("cy", "GB");
    private static final String FILE_NAME = "fileName";
    private static final Set<String> PROCESSABLE_TEMPLATES = Sets.newHashSet(
            TRANSPARENCY_TEMPLATE_IDENTIFIER,
            TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA,
            TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH,
            TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA,
            PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER,
            PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA,
            PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH,
            PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA
    );

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;
    @Inject
    private PdfHelper pdfHelper;

    @Override
    public boolean canProcess(final JsonEnvelope envelope) {
        final String templateIdentifier = getTemplateIdentifier(envelope);
        return PROCESSABLE_TEMPLATES.contains(templateIdentifier);
    }

    @Override
    public void process(final JsonEnvelope envelope) {
        if (isDocumentAvailablePublicEvent(envelope)) {
            processDocumentAvailable(envelope);
        }

        if (isGenerationFailedPublicEvent(envelope)) {
            processGenerationFailed(envelope);
        }
    }

    @SuppressWarnings("squid:S1188")
    private void processDocumentAvailable(final JsonEnvelope envelope) {
        final JsonObject documentAvailablePayload = envelope.payloadAsJsonObject();
        final String templateIdentifier = getTemplateIdentifier(envelope);

        if (documentAvailablePayload.containsKey("sourceUri")) {
            dispatchIngestFile(envelope, documentAvailablePayload);
        }

        final String reportId = getSourceCorrelationId(envelope);
        final Optional<JsonObjectBuilder> documentMetadata = getDocumentMetadata(documentAvailablePayload);

        documentMetadata.ifPresent(docMetadata -> {

                switch (templateIdentifier) {
                    case TRANSPARENCY_TEMPLATE_IDENTIFIER:
                        docMetadata.add(FILE_NAME, "transparency-report-full-english.pdf");
                        updateTransparencyReportMetadata(envelope, reportId, ENGLISH, docMetadata.build());
                        break;
                    case TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH:
                        docMetadata.add(FILE_NAME, "transparency-report-full-welsh.pdf");
                        updateTransparencyReportMetadata(envelope, reportId, WELSH, docMetadata.build());
                        break;
                    case TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA:
                        docMetadata.add(FILE_NAME, "transparency-report-delta-english.pdf");
                        updateTransparencyReportMetadata(envelope, reportId, ENGLISH, docMetadata.build());
                        break;
                    case TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA:
                        docMetadata.add(FILE_NAME, "transparency-report-delta-welsh.pdf");
                        updateTransparencyReportMetadata(envelope, reportId, WELSH, docMetadata.build());
                        break;
                    case PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER:
                        docMetadata.add(FILE_NAME, "press-transparency-report-full-english.pdf");
                        updatePressTransparencyReportMetadata(envelope, reportId, docMetadata.build());
                        break;
                    case PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA:
                        docMetadata.add(FILE_NAME, "press-transparency-report-delta-english.pdf");
                        updatePressTransparencyReportMetadata(envelope, reportId, docMetadata.build());
                        break;
                    case PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH:
                        docMetadata.add(FILE_NAME, "press-transparency-report-full-welsh.pdf");
                        updatePressTransparencyReportMetadata(envelope, reportId, docMetadata.build());
                        break;
                    case PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA:
                        docMetadata.add(FILE_NAME, "press-transparency-report-delta-welsh.pdf");
                        updatePressTransparencyReportMetadata(envelope, reportId, docMetadata.build());
                        break;
                    default:
                        LOGGER.info("unrecognized template {}", templateIdentifier);
                }
            });
    }

    private void processGenerationFailed(final JsonEnvelope envelope) {
        final String templateIdentifier = getTemplateIdentifier(envelope);

        if (isPressTransparencyReport(templateIdentifier)) {
            sendPressTransparencyReportFailedCommand(envelope);
        }

        if (isTransparencyReport(templateIdentifier)) {
            sendTransparencyReportFailedCommand(envelope);
        }

        final JsonObject payload = envelope.payloadAsJsonObject();
        LOGGER.error("error generating document for report {} {}.  {}",
                getSourceCorrelationId(envelope),
                templateIdentifier,
                payload.getString("reason")
        );
    }

    private void dispatchIngestFile(final JsonEnvelope envelope, final JsonObject payload) {
        final String blobFileId = payload.getString("blobFileId");
        final String sourceUri = payload.getString("sourceUri");
        final String sourceCorrelationId = getSourceCorrelationId(envelope);
        final String templateIdentifier = getTemplateIdentifier(envelope);
        final JsonObject ingestPayload = createObjectBuilder()
                .add("fileId", blobFileId)
                .add("correlationId", sourceCorrelationId)
                .add("filename", TEMPLATE_TO_FILENAME.getOrDefault(templateIdentifier, templateIdentifier + ".pdf"))
                .add("sourceUri", sourceUri)
                .build();
        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_INGEST_FILE),
                ingestPayload));
        LOGGER.info("Dispatched {} for blobFileId='{}' template='{}'", COMMAND_INGEST_FILE, blobFileId, templateIdentifier);
    }

    private Optional<JsonObjectBuilder> getDocumentMetadata(final JsonObject payload) {
        if (!payload.containsKey("sourceUri")) {
            return Optional.empty();
        }
        final String blobFileId = payload.getString("blobFileId");
        final String sourceUri = payload.getString("sourceUri");
        try {
            final byte[] documentBytes = downloadBytes(URI.create(sourceUri));
            return Optional.ofNullable(toDocumentMetadata(blobFileId, documentBytes));
        } catch (final IOException e) {
            LOGGER.error("error downloading document from sourceUri '{}'", sourceUri, e);
            return Optional.empty();
        }
    }

    private byte[] downloadBytes(final URI sourceUri) throws IOException {
        try (final InputStream inputStream = sourceUri.toURL().openStream()) {
            return inputStream.readAllBytes();
        }
    }

    private JsonObjectBuilder toDocumentMetadata(final String fileId,
                                                 final byte[] documentBytes) {
        try {
            return createObjectBuilder()
                    .add("numberOfPages", pdfHelper.getDocumentPageCount(documentBytes))
                    .add("fileSize", documentBytes.length)
                    .add("fileId", fileId);
        } catch (IOException e) {
            LOGGER.error("error building document metadata", e);
            return null;
        }
    }

    private void updateTransparencyReportMetadata(final JsonEnvelope envelope,
                                                  final String reportId,
                                                  final Locale locale,
                                                  final JsonObject documentMetadata) {

        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName("sjp.command.update-transparency-report-data"),
                createObjectBuilder()
                        .add("transparencyReportId", reportId)
                        .add("metadata", documentMetadata)
                        .add("language", locale.getLanguage())
                        .build());
        sender.send(envelopeToSend);
    }

    private void updatePressTransparencyReportMetadata(final JsonEnvelope envelope,
                                                       final String reportId,
                                                       final JsonObject documentMetadata) {

        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName("sjp.command.update-press-transparency-report-data"),
                createObjectBuilder()
                        .add("pressTransparencyReportId", reportId)
                        .add("metadata", documentMetadata)
                        .build());
        sender.send(envelopeToSend);
    }

    private boolean isTransparencyReport(final String templateIdentifier) {
        return TRANSPARENCY_TEMPLATE_IDENTIFIER.equalsIgnoreCase(templateIdentifier) ||
                TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH.equalsIgnoreCase(templateIdentifier) ||
                TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA.equalsIgnoreCase(templateIdentifier) ||
                TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA.equalsIgnoreCase(templateIdentifier);
    }

    private boolean isPressTransparencyReport(final String templateIdentifier) {

        return Stream.of(PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER,
                PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_DELTA,
                PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH,
                PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH_DELTA).anyMatch(templateIdentifier::equalsIgnoreCase);
    }

    private void sendPressTransparencyReportFailedCommand(final JsonEnvelope envelope) {
        final String pressTransparencyReportId = getSourceCorrelationId(envelope);
        final JsonObject commandPayload = createObjectBuilder()
                .add("pressTransparencyReportId", pressTransparencyReportId)
                .build();

        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_PRESS_TRANSPARENCY_REPORT_FAILED),
                commandPayload
        );
        sender.send(envelopeToSend);
    }

    private void sendTransparencyReportFailedCommand(final JsonEnvelope envelope) {
        final String transparencyReportId = getSourceCorrelationId(envelope);
        final String templateIdentifier = getTemplateIdentifier(envelope);
        final JsonObject commandPayload = createObjectBuilder()
                .add("transparencyReportId", transparencyReportId)
                .add("templateIdentifier", templateIdentifier)
                .build();

        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_TRANSPARENCY_REPORT_FAILED),
                commandPayload
        );
        sender.send(envelopeToSend);
    }
}
