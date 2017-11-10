package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseDocumentUploadedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocumentUploadedProcessor.class);

    private static final String LIFECYCLE_ADD_CASE_MATERIAL = "lifecycle.command.add-case-material";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private FileService fileService;

    @Inject
    private FileSender fileSender;

    @Handles("structure.events.case-document-uploaded")
    public void handleCaseDocumentUploaded(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = UUID.fromString(jsonEnvelope.payloadAsJsonObject().getString(EventProcessorConstants.CASE_ID));
        final UUID documentReference = UUID.fromString(jsonEnvelope.payloadAsJsonObject().getString(EventProcessorConstants.DOCUMENT_REFERENCE));
        final String documentType = jsonEnvelope.payloadAsJsonObject().getString(EventProcessorConstants.DOCUMENT_TYPE);

        LOGGER.trace("Case document uploaded, start processing, for case: {} of type: {} with id: {}", caseId, documentType, documentReference);

        final FileReference fileReference = retrieveFileReference(documentReference);
        final String originalFileName = fileReference.getMetadata().getString(EventProcessorConstants.FILENAME);
        final InputStream fileContent = fileReference.getContentStream();

        LOGGER.debug("Case document uploaded with metadata: {}", fileReference.getMetadata());

        final FileData alfrescoFileData = fileSender.send(originalFileName, fileContent);
        LOGGER.debug("Case document uploaded with id: {} and mime: {}", alfrescoFileData.fileId(), alfrescoFileData.fileMimeType());

        sender.sendAsAdmin(
                enveloper.withMetadataFrom(jsonEnvelope, LIFECYCLE_ADD_CASE_MATERIAL)
                        .apply(createAddCaseMaterialModel(
                                caseId,
                                documentReference,
                                documentType,
                                originalFileName,
                                alfrescoFileData)));

        LOGGER.info("Case document uploaded processed for case: {} of type: {} with reference: {} and alfresco id: {}",
                caseId, documentType, documentReference, alfrescoFileData.fileId());

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, "public.structure.case-document-uploaded")
                                .apply(createObjectBuilder()
                                        .add("documentId", documentReference.toString())
                                        .build())
        );

    }

    private JsonObject createAddCaseMaterialModel(UUID caseId, UUID documentReference, String documentType, String fileName, FileData fileData) {
        return createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("material", createObjectBuilder()
                                .add("id", documentReference.toString())
                                .add("fileReference", fileData.fileId())
                                .add("mimeType", fileData.fileMimeType())
                                .add("originalFileName", fileName)
                                .add("documentType", documentType)
                )
                .build();
    }

    private FileReference retrieveFileReference(UUID documentReference) {
        try {
            Optional<FileReference> fileReference = fileService.retrieve(documentReference);
            if (!fileReference.isPresent()) {
                LOGGER.warn("No file with id: {}", documentReference);
                throw new IllegalStateException("No file with id: " + documentReference);
            }
            return fileReference.get();
        } catch (FileServiceException e) {
            LOGGER.warn("Could not retrieve a file of id: {}", documentReference);
            throw new IllegalStateException(e);
        }
    }
}
