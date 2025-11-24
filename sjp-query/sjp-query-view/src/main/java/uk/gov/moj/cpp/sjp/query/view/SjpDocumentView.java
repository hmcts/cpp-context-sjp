package uk.gov.moj.cpp.sjp.query.view;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_VIEW)
public class SjpDocumentView {

    @Inject
    private Enveloper enveloper;

    @Inject
    private CaseService caseService;

    @Handles("sjp.query.case-document")
    public JsonEnvelope findCaseDocument(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString("caseId"));
        final UUID documentId = UUID.fromString(envelope.payloadAsJsonObject().getString("documentId"));

        final Optional<JsonObject> caseDocument = caseService.findCaseDocument(caseId, documentId)
                .map(document -> Json.createObjectBuilder().add("caseDocument",
                        Json.createObjectBuilder()
                                .add("id", document.getId().toString())
                                .add("materialId", document.getMaterialId().toString())
                                .add("documentType", document.getDocumentType())
                                .add("documentNumber", document.getDocumentNumber())).build());

        return enveloper.withMetadataFrom(envelope, "sjp.query.case-document").apply(caseDocument.orElse(null));
    }
}
