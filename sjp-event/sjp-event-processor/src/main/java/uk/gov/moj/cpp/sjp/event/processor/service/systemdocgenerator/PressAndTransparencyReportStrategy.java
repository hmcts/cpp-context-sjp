package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.utils.PdfHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
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
    private FileRetriever fileRetriever;
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

        try {
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
        } catch (FileServiceException e) {
            LOGGER.error("error retrieving document from file service", e);
        }
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

    private Optional<JsonObjectBuilder> getDocumentMetadata(final JsonObject payload) throws FileServiceException {
        final String fileId = payload.getString("documentFileServiceId");
        final Optional<FileReference> documentFileReference = fileRetriever.retrieve(fromString(fileId));
        return documentFileReference.map(this::toDocumentBytes).
                map(documentBytes -> toDocumentMetadata(fileId, documentBytes));
    }

    @SuppressWarnings("squid:S2674")
    private byte[] toDocumentBytes(final FileReference fileReference) {
        try {
            final InputStream contentStream = fileReference.getContentStream();
            final byte[] docBytes = new byte[contentStream.available()];
            contentStream.read(docBytes);
            return docBytes;
        } catch (IOException e) {
            LOGGER.error("error accessing document content for file " + fileReference.getFileId().toString(), e);
            return new byte[0];
        } finally {
            try {
                fileReference.close();
            } catch (Exception e) {
                LOGGER.error("error disposing file reference", e);
            }
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
