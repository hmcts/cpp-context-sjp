package uk.gov.moj.cpp.sjp.event.processor;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseDocumentUpdatedProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocumentUpdatedProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    private static final String PUBLIC_CASE_DOCUMENT_ALREADY_ADDED_PUBLIC_EVENT = "public.sjp.case-document-already-exists";
    private static final String PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT = "public.sjp.case-document-added";

    @Handles(CaseDocumentAdded.EVENT_NAME)
    public void handleCaseDocumentAdded(final JsonEnvelope jsonEnvelope) {
        final String caseId = jsonEnvelope.payloadAsJsonObject().getString(EventProcessorConstants.CASE_ID);
        LOGGER.info("Received Case document added message for caseId {}", caseId);

        final JsonObject publicEventPayload = getCaseDocumentPublicEventPayload(caseId, jsonEnvelope.payloadAsJsonObject().getJsonObject(EventProcessorConstants.CASE_DOCUMENT));
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT).apply(publicEventPayload));
    }

    @Handles("sjp.events.case-document-already-exists")
    public void handleDuplicateCaseDocumentAddedEvent(final JsonEnvelope jsonEnvelope) {
        final String caseId = jsonEnvelope.payloadAsJsonObject().getString(EventProcessorConstants.CASE_ID);
        final JsonObject caseDocument = jsonEnvelope.payloadAsJsonObject().getJsonObject(EventProcessorConstants.CASE_DOCUMENT);

        final JsonObject publicEventPayload = getCaseDocumentPublicEventPayload(caseId, caseDocument);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_CASE_DOCUMENT_ALREADY_ADDED_PUBLIC_EVENT).apply(publicEventPayload));
    }

    private JsonObject getCaseDocumentPublicEventPayload(String caseId, JsonObject caseDocument) {
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
