package uk.gov.moj.cpp.sjp.event.processor.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class MaterialService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public JsonObject getMaterialMetadata(final UUID materialId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("materialId", materialId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "material.query.material-metadata").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }
}
