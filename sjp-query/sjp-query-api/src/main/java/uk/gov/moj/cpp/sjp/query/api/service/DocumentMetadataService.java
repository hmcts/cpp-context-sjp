package uk.gov.moj.cpp.sjp.query.api.service;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.query.api.helper.JsonHelper.getPayload;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class DocumentMetadataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public Optional<JsonObject> getMaterialMetadata(final UUID materialId, final JsonEnvelope envelope) {
        final JsonObject materialMetadataQueryParams = createObjectBuilder().add("materialId", materialId.toString()).build();
        final JsonEnvelope materialMetadata = requester.requestAsAdmin(enveloper.withMetadataFrom(envelope, "material.query.material-metadata").apply(materialMetadataQueryParams));

        return getPayload(materialMetadata)
                .map(metadata -> createObjectBuilder().add("caseDocumentMetadata",
                        createObjectBuilder()
                                .add("fileName", metadata.getString("fileName"))
                                .add("mimeType", metadata.getString("mimeType"))
                                .add("addedAt", metadata.getString("materialAddedDate")))
                        .build());
    }
}
