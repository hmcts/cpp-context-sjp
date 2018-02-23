package uk.gov.moj.cpp.sjp.event.processor.listener;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseDocumentUpdatedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocumentUpdatedListener.class);

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    static final String PUBLIC_CASE_DOCUMENT_ALREADY_ADDED_PUBLIC_EVENT = "public.sjp.case-document-already-exists";
    private static final String PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT = "public.sjp.case-document-added";

    @Handles("sjp.events.case-document-added")
    public void handleCaseDocumentAdded(final JsonEnvelope jsonEnvelope) {
        final String caseId = jsonEnvelope.payloadAsJsonObject().getString(EventProcessorConstants.CASE_ID);
        LOGGER.info("Received Case document added message for caseId {}", caseId);

        final JsonObject publicEventPayload = getCaseDocumentPublicEventPayload(caseId, jsonEnvelope.payloadAsJsonObject().getJsonObject(EventProcessorConstants.CASE_DOCUMENT));
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT).apply(publicEventPayload));
    }

    @Handles("sjp.events.case-document-already-exists")
    public void handleDuplicateCaseDocumentAddedEvent(final JsonEnvelope jsonEnvelope) {
        final String caseId = jsonEnvelope.payloadAsJsonObject().getString(EventProcessorConstants.CASE_ID);
        JsonObject caseDocument = jsonEnvelope.payloadAsJsonObject().getJsonObject(EventProcessorConstants.CASE_DOCUMENT);

        final JsonObject publicEventPayload = getCaseDocumentPublicEventPayload(caseId, caseDocument);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_CASE_DOCUMENT_ALREADY_ADDED_PUBLIC_EVENT).apply(publicEventPayload));
    }

    JsonObject getCaseDocumentPublicEventPayload(String caseId, JsonObject caseDocument) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder()
                .add(EventProcessorConstants.CASE_ID, caseId)
                .add(EventProcessorConstants.ID, caseDocument.getString(EventProcessorConstants.ID))
                .add(EventProcessorConstants.MATERIAL_ID, caseDocument.getString(EventProcessorConstants.MATERIAL_ID));

        if (caseDocument.containsKey(EventProcessorConstants.DOCUMENT_TYPE)) {
            jsonObjectBuilder.add(EventProcessorConstants.DOCUMENT_TYPE, caseDocument.getString(EventProcessorConstants.DOCUMENT_TYPE));
        }

        return jsonObjectBuilder.build();
    }
}
