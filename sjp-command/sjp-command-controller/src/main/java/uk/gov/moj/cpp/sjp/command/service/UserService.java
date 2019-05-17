package uk.gov.moj.cpp.sjp.command.service;

import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class UserService {

    @Inject
    @ServiceComponent(COMMAND_CONTROLLER)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public JsonObject getCallingUserDetails(final JsonEnvelope originalEnvelope) {
        final UUID callerId = getCallerId(originalEnvelope);
        final JsonObject queryParams = createObjectBuilder().add("userId", callerId.toString()).build();
        final JsonEnvelope query = enveloper.withMetadataFrom(originalEnvelope, "usersgroups.get-user-details").apply(queryParams);

        return requester.requestAsAdmin(query).payloadAsJsonObject();
    }

    private UUID getCallerId(final JsonEnvelope envelope) {
        return envelope.metadata().userId().map(UUID::fromString)
                .orElseThrow(() -> new IllegalStateException(format("Envelope with id %s does not contains user id", envelope.metadata().id())));
    }
}