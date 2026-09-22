package uk.gov.moj.cpp.sjp.event.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
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

        // Exactly one of these is set - the event schema's oneOf enforces that, and
        // JsonSchemaValidationInterceptor applies it on the way in. A blob-addressed document is
        // forwarded onward as a uri; SJP never reads the document itself either way.
        final String documentReference = valueOrNull(payload, DOCUMENT_REFERENCE);
        final String documentReferenceUri = valueOrNull(payload, DOCUMENT_REFERENCE_URI);
        final boolean addressedByUri = isNull(documentReference);

        final JsonObjectBuilder fileUploadedEventPayload = createObjectBuilder()
                .add(CASE_ID, caseId.toString());
        final JsonObjectBuilder uploadFilePayload = createObjectBuilder()
                .add(MATERIAL_ID, randomUUID().toString());
        final JsonObjectBuilder sjpMetadata = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(DOCUMENT_TYPE, documentType);

        if (addressedByUri) {
            fileUploadedEventPayload.add(DOCUMENT_URI, documentReferenceUri);
            // Material rejects a command carrying more than one file reference, so send only this one.
            uploadFilePayload.add(FILE_URI, documentReferenceUri);
            sjpMetadata.add(DOCUMENT_URI, documentReferenceUri);
        } else {
            fileUploadedEventPayload.add(DOCUMENT_ID, documentReference);
            uploadFilePayload.add(FILE_SERVICE_ID, documentReference);
            sjpMetadata.add(DOCUMENT_ID, documentReference);
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

            // A blob-addressed document has no file service id to become the case document's
            // identity, and case_document.id is a uuid primary key. Derive a stable v3 uuid from
            // the blob uri: the same uri always yields the same id, so a redelivered
            // material.material-added is caught by the aggregate's duplicate check exactly as it is
            // on the file-service path.
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
