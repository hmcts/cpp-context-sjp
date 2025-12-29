package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DOCUMENT_REFERENCE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DOCUMENT_TYPE;
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
        final UUID documentReference = UUID.fromString(payload.getString(DOCUMENT_REFERENCE));
        final String documentType = payload.getString(DOCUMENT_TYPE);

        final JsonObject fileUploadedEventPayload = createObjectBuilder()
                .add("documentId", documentReference.toString())
                .add("caseId", caseId.toString())
                .build();

        sender.send(enveloper.withMetadataFrom(caseDocumentUploadedEvent, "public.sjp.case-document-uploaded")
                .apply(fileUploadedEventPayload));

        final JsonObject uploadFilePayload = createObjectBuilder()
                .add("materialId", randomUUID().toString())
                .add("fileServiceId", documentReference.toString())
                .build();

        final JsonObject sjpMetadata = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("documentId", documentReference.toString())
                .add("documentType", documentType)
                .build();

        sender.send(metadataHelper.envelopeWithCustomMetadata(
                metadataFrom(caseDocumentUploadedEvent.metadata()).withName("material.command.upload-file").build(),
                sjpMetadata,
                uploadFilePayload));
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
            final UUID caseId = UUID.fromString(sjpMetadata.get().getString("caseId"));
            final UUID documentId = UUID.fromString(sjpMetadata.get().getString("documentId"));
            final String documentType = sjpMetadata.get().getString("documentType");

            LOGGER.info("Material {} is a {} for sjp case {}", materialId, documentType, caseId);

            final JsonObject payload = createObjectBuilder()
                    .add("id", documentId.toString())
                    .add("caseId", caseId.toString())
                    .add("materialId", materialId.toString())
                    .add("documentType", documentType)
                    .build();

            sender.send(enveloper.withMetadataFrom(materialAddedEvent, "sjp.command.add-case-document").apply(payload));
        }
        //TODO remove - ATCM-4293
        else if (sjpProcessId.isPresent()) {
            LOGGER.info("Legacy event. Material {} is for sjp process {}", materialId, sjpProcessId.get());
            sjpProcessManagerService.signalUploadFileProcess(materialAddedEvent, sjpProcessId.get(), materialId);
        } else {
            LOGGER.info("Material {} is not for sjp", materialId);
        }
    }
}
