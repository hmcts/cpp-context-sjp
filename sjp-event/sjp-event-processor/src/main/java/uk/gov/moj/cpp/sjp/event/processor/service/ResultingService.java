package uk.gov.moj.cpp.sjp.event.processor.service;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class ResultingService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public JsonObject getCaseDecisions(final UUID caseId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder().add("caseId", caseId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "resulting.query.case-decisions").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }
}
