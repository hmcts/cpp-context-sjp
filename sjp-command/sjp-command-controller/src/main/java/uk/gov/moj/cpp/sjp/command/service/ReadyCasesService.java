package uk.gov.moj.cpp.sjp.command.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReadyCasesService {

    @Inject
    @ServiceComponent(COMMAND_CONTROLLER)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public JsonObject getReadyCasesAssignedToUser(final UUID userId, final JsonEnvelope originalEnvelope) {
        final JsonObject queryParams = createObjectBuilder().add("assigneeId", userId.toString()).build();
        final JsonEnvelope query = enveloper.withMetadataFrom(originalEnvelope, "sjp.query.ready-cases").apply(queryParams);

        return requester.requestAsAdmin(query).payloadAsJsonObject();
    }
}