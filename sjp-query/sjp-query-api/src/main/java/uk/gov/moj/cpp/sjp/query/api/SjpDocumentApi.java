package uk.gov.moj.cpp.sjp.query.api;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.query.api.helper.JsonHelper.getPayload;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.api.service.DocumentMetadataService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_API)
public class SjpDocumentApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Requester requester;

    @Inject
    private DocumentMetadataService documentMetadataService;

    @Handles("sjp.query.case-documents")
    public JsonEnvelope getCaseDocuments(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-document")
    public JsonEnvelope getCaseDocument(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-document-metadata")
    public JsonEnvelope getCaseDocumentMetadata(final JsonEnvelope query) {
        final UUID caseId = UUID.fromString(query.payloadAsJsonObject().getString("caseId"));
        final UUID documentId = UUID.fromString(query.payloadAsJsonObject().getString("documentId"));

        final JsonEnvelope documentDetails = getDocumentDetails(caseId, documentId, query);

        final JsonObject documentMetadata = getPayload(documentDetails)
                .map(document -> document.getJsonObject("caseDocument").getString("materialId"))
                .map(UUID::fromString)
                .flatMap(materialId -> documentMetadataService.getMaterialMetadata(materialId, documentDetails))
                .orElse(null);

        return enveloper.withMetadataFrom(documentDetails, "sjp.query.case-document-metadata").apply(documentMetadata);
    }

    /**
     * Handler returns document details and not document content. This is consequence of non
     * framework endpoint which uses standard framework interceptors. Handler is invoked at the end
     * of programmatically invoked interceptor chain, see DefaultQueryApiCasesCaseIdDocumentsDocumentIdContentResource.
     */
    @Handles("sjp.query.case-document-content")
    public JsonEnvelope getCaseDocumentDetails(final JsonEnvelope query) {
        final UUID caseId = UUID.fromString(query.payloadAsJsonObject().getString("caseId"));
        final UUID documentId = UUID.fromString(query.payloadAsJsonObject().getString("documentId"));

        return getDocumentDetails(caseId, documentId, query);
    }

    private JsonEnvelope getDocumentDetails(final UUID caseId, final UUID documentId, final JsonEnvelope sourceEnvelope) {
        final JsonObject documentQueryParams = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("documentId", documentId.toString())
                .build();

        return requester.request(enveloper.withMetadataFrom(sourceEnvelope, "sjp.query.case-document").apply(documentQueryParams));
    }
}
