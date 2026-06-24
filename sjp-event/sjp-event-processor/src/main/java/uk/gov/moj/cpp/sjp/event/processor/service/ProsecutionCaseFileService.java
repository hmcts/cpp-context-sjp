package uk.gov.moj.cpp.sjp.event.processor.service;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class ProsecutionCaseFileService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public Optional<JsonObject> getCaseFileDetails(final UUID caseId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("caseId", caseId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "prosecutioncasefile.query.case").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payload() != JsonValue.NULL
                ? ofNullable(response.payloadAsJsonObject())
                : empty();
    }

    public Optional<JsonObject> getCaseFileDefendantDetails(final UUID caseId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("caseId", caseId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "prosecutioncasefile.query.case").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payload() != JsonValue.NULL
                ? ofNullable(response.payloadAsJsonObject().getJsonArray("defendants").getJsonObject(0))
                : empty();
    }

}
