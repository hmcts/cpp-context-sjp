package uk.gov.moj.cpp.sjp.event.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DOCUMENT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DOCUMENT_REFERENCE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DOCUMENT_REFERENCE_URI;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DOCUMENT_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DOCUMENT_URI;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.FILE_SERVICE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.FILE_URI;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.MATERIAL_ID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.activiti.SjpProcessManagerService;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseDocumentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocumentProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private MetadataHelper metadataHelper;

    @Inject
    private SjpProcessManagerService sjpProcessManagerService;

    @Handles("sjp.events.case-document-uploaded")
    public void handleCaseDocumentUploaded(final JsonEnvelope caseDocumentUploadedEvent) {
        final JsonObject payload = caseDocumentUploadedEvent.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
        final String documentType = payload.getString(DOCUMENT_TYPE);

        // Both may now arrive together: staging-dvla derives the case document's uuid from the uri
        // and sends the pair, so a uuid being present no longer means the document is file-service
        // addressed. The uri is what says "blob" - test for that directly. Getting this backwards
        // sends material a fileServiceId no file service has ever heard of.
        final String documentReference = valueOrNull(payload, DOCUMENT_REFERENCE);
        final String documentReferenceUri = valueOrNull(payload, DOCUMENT_REFERENCE_URI);
        final boolean addressedByUri = nonNull(documentReferenceUri);

        final JsonObjectBuilder fileUploadedEventPayload = createObjectBuilder()
                .add(CASE_ID, caseId.toString());
        final JsonObjectBuilder uploadFilePayload = createObjectBuilder()
                .add(MATERIAL_ID, randomUUID().toString());
        final JsonObjectBuilder sjpMetadata = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(DOCUMENT_TYPE, documentType);

        // The public event and the sjp metadata carry everything we were given. The metadata one
        // matters most: handleMaterialAdded reads documentId back out of it to build
        // sjp.command.add-case-document, so the uuid has to ride along or that hop falls back to
        // deriving one and the caller's id is lost.
        if (nonNull(documentReference)) {
            fileUploadedEventPayload.add(DOCUMENT_ID, documentReference);
            sjpMetadata.add(DOCUMENT_ID, documentReference);
        }
        if (addressedByUri) {
            fileUploadedEventPayload.add(DOCUMENT_URI, documentReferenceUri);
            sjpMetadata.add(DOCUMENT_URI, documentReferenceUri);
        }

        // Material is the exception: material.command.upload-file is an exclusive oneOf and its
        // handler throws on more than one reference, so exactly one goes on that payload. The uri
        // wins when there is one - it is the only form material can actually read.
        if (addressedByUri) {
            uploadFilePayload.add(FILE_URI, documentReferenceUri);
        } else {
            uploadFilePayload.add(FILE_SERVICE_ID, documentReference);
        }

        sender.send(enveloper.withMetadataFrom(caseDocumentUploadedEvent, "public.sjp.case-document-uploaded")
                .apply(fileUploadedEventPayload.build()));

        sender.send(metadataHelper.envelopeWithCustomMetadata(
                metadataFrom(caseDocumentUploadedEvent.metadata()).withName("material.command.upload-file").build(),
                sjpMetadata.build(),
                uploadFilePayload.build()));
    }

    private static String valueOrNull(final JsonObject payload, final String field) {
        return payload.containsKey(field) && !payload.isNull(field) ? payload.getString(field) : null;
    }

    @Handles("sjp.events.case-document-upload-rejected")
    public void handleCaseDocumentUploadRejected(final JsonEnvelope uploadRejectedEvent) {
        sender.send(envelopeFrom(
                metadataFrom(uploadRejectedEvent.metadata())
                        .withName("public.sjp.events.case-document-upload-rejected"),
                uploadRejectedEvent.payloadAsJsonObject()
        ));
    }

    @Handles("material.material-added")
    public void handleMaterialAdded(final JsonEnvelope materialAddedEvent) {
        final UUID materialId = UUID.fromString(materialAddedEvent.payloadAsJsonObject().getString(MATERIAL_ID));
        final Optional<JsonObject> sjpMetadata = metadataHelper.getSjpMetadata(materialAddedEvent);
        final Optional<String> sjpProcessId = metadataHelper.getSjpProcessId(materialAddedEvent);

        if (sjpMetadata.isPresent()) {
            final UUID caseId = UUID.fromString(sjpMetadata.get().getString(CASE_ID));
            final String documentType = sjpMetadata.get().getString(DOCUMENT_TYPE);
            final String documentId = valueOrNull(sjpMetadata.get(), DOCUMENT_ID);
            final String documentUri = valueOrNull(sjpMetadata.get(), DOCUMENT_URI);

            LOGGER.info("Material {} is a {} for sjp case {}", materialId, documentType, caseId);

            // Normally the id was supplied: staging-dvla derives it from the uri with this exact
            // algorithm and sends it on the command. The derivation below is the fallback for a
            // uri-only payload - a caller not yet updated, or a pre-change event replayed from the
            // store. Keep the two implementations identical; see
            // cpp-context-staging-dvla SystemDocGeneratorEventProcessor.
            final String caseDocumentId = getCaseDocumentId(documentId, documentUri);

            final JsonObjectBuilder payload = createObjectBuilder()
                    .add(ID, caseDocumentId)
                    .add(CASE_ID, caseId.toString())
                    .add(MATERIAL_ID, materialId.toString())
                    .add(DOCUMENT_TYPE, documentType);

            // Carried onward so the case document records where it came from, and so the calling
            // context can correlate the filing back to the blob it supplied.
            if (nonNull(documentUri)) {
                payload.add(DOCUMENT_URI, documentUri);
            }

            sender.send(enveloper.withMetadataFrom(materialAddedEvent, "sjp.command.add-case-document").apply(payload.build()));
        }
        //TODO remove - ATCM-4293
        else if (sjpProcessId.isPresent()) {
            LOGGER.info("Legacy event. Material {} is for sjp process {}", materialId, sjpProcessId.get());
            sjpProcessManagerService.signalUploadFileProcess(materialAddedEvent, sjpProcessId.get(), materialId);
        } else {
            LOGGER.info("Material {} is not for sjp", materialId);
        }
    }

    private static String getCaseDocumentId(final String documentId, final String documentUri) {
        return  nonNull(documentId) ? documentId : nameUUIDFromBytes(documentUri.getBytes(UTF_8)).toString();
    }
}
