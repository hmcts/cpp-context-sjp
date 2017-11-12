package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.DOCUMENT_REFERENCE;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.DOCUMENT_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.MATERIAL_ID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.activiti.SjpProcessManagerService;
import uk.gov.moj.cpp.sjp.event.processor.utils.ProcessIdHelper;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseDocumentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocumentProcessor.class);

    @Inject
    private SjpProcessManagerService sjpProcessManagerService;

    @Handles("structure.events.case-document-uploaded")
    public void handleCaseDocumentUploaded(final JsonEnvelope caseDocumentUploadedEvent) {
        final JsonObject payload = caseDocumentUploadedEvent.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
        final UUID documentReference = UUID.fromString(payload.getString(DOCUMENT_REFERENCE));
        final String documentType = payload.getString(DOCUMENT_TYPE);

        sjpProcessManagerService.startUploadFileProcess(caseDocumentUploadedEvent, caseId, documentReference, documentType);
    }

    @Handles("material.material-added")
    public void handleMaterialAdded(final JsonEnvelope materialAddedEvent) {
        final UUID materialId = UUID.fromString(materialAddedEvent.payloadAsJsonObject().getString(MATERIAL_ID));

        ProcessIdHelper.decodeProcessId(materialAddedEvent)
                .ifPresent(sjpProcessId -> sjpProcessManagerService.signalUploadFileProcess(materialAddedEvent, sjpProcessId, materialId));
    }
}
