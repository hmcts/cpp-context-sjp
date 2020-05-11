package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
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

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class SysDocGeneratorProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysDocGeneratorProcessor.class);

    private static final String DOCUMENT_AVAILABLE_EVENT_NAME = "public.systemdocgenerator.events.document-available";

    private static final String DOCUMENT_GENERATION_FAILED_EVENT_NAME = "public.systemdocgenerator.events.generation-failed";

    private static final String TRANSPARENCY_TEMPLATE_IDENTIFIER = "PendingCasesEnglish";
    private static final String TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH = "PendingCasesWelsh";
    private static final String PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER = "PressPendingCasesEnglish";

    private static final Locale ENGLISH = new Locale("en", "GB");
    private static final Locale WELSH = new Locale("cy", "GB");
    private static final String SJP_SOURCE = "sjp";
    private static final String FILE_NAME = "fileName";

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private FileRetriever fileRetriever;

    @Inject
    private PdfHelper pdfHelper;


    @Handles(DOCUMENT_AVAILABLE_EVENT_NAME)
    public void handleDocumentAvailableEvent(final JsonEnvelope documentAvailableEvent) {
        final JsonObject documentAvailablePayload = documentAvailableEvent.payloadAsJsonObject();
        final String originatingSource = documentAvailablePayload.getString("originatingSource","");
        if(SJP_SOURCE.equalsIgnoreCase(originatingSource)) {
            try {
                final Optional<JsonObjectBuilder> documentMetadata = getDocumentMetadata(documentAvailablePayload);
                final String reportId = documentAvailablePayload.getString("sourceCorrelationId");
                documentMetadata.ifPresent(docMetadata -> {
                    final String templateIdentifier = documentAvailablePayload.getString("templateIdentifier");
                    switch (templateIdentifier) {
                        case TRANSPARENCY_TEMPLATE_IDENTIFIER:
                            docMetadata.add(FILE_NAME, "transparency-report-english.pdf");
                            updateTransparencyReportMetadata(documentAvailableEvent, reportId, ENGLISH, docMetadata.build());
                            break;
                        case TRANSPARENCY_TEMPLATE_IDENTIFIER_WELSH:
                            docMetadata.add(FILE_NAME, "transparency-report-welsh.pdf");
                            updateTransparencyReportMetadata(documentAvailableEvent, reportId, WELSH ,docMetadata.build());
                            break;
                        case PRESS_TRANSPARENCY_TEMPLATE_IDENTIFIER:
                            docMetadata.add(FILE_NAME, "press-transparency-report.pdf");
                            updatePressTransparencyReportMetadata(documentAvailableEvent, reportId, docMetadata.build());
                            break;
                        default:
                            LOGGER.info("unrecognized template {}", templateIdentifier);
                    }
                });
            } catch (FileServiceException e) {
                LOGGER.error("error retrieving document from file service", e);
            }
        } else {
            LOGGER.debug("document generated for another context, ignoring");
        }
    }

    private Optional<JsonObjectBuilder> getDocumentMetadata(final JsonObject documentAvailablePayload) throws FileServiceException {
        final String fileId = documentAvailablePayload.getString("documentFileServiceId");
        final Optional<FileReference> documentFileReference = fileRetriever.retrieve(fromString(fileId));
        return documentFileReference.
                map(this::toDocumentBytes).
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
                    LOGGER.error("error disposing file reference",e);
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

    @Handles(DOCUMENT_GENERATION_FAILED_EVENT_NAME)
    public void handleDocumentGenerationFailedEvent(final JsonEnvelope documentGenerationFailedEvent) {
        final JsonObject documentGenerationFailedPayload = documentGenerationFailedEvent.payloadAsJsonObject();
        final String source = documentGenerationFailedPayload.getString("originatingSource", "");
        if(SJP_SOURCE.equalsIgnoreCase(source)) {
            LOGGER.error("error generating document for report {} {}.  {}",
                    documentGenerationFailedPayload.getString("sourceCorrelationId"),
                    documentGenerationFailedPayload.getString("templateIdentifier"),
                    documentGenerationFailedPayload.getString("reason")
            );
        }
    }
}