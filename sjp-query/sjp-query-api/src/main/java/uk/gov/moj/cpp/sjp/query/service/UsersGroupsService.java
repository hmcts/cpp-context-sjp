package uk.gov.moj.cpp.sjp.query.service;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.exception.UserNotFoundException;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class UsersGroupsService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public JsonObject getUserDetails(final UUID userId, final JsonEnvelope envelope) {

        final JsonObject payload = createObjectBuilder().add("userId", userId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "usersgroups.get-user-details").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        if (JsonValue.NULL.equals(response.payload())) {
            throw new UserNotFoundException(userId);
        }
        return response.payloadAsJsonObject();
    }
}
