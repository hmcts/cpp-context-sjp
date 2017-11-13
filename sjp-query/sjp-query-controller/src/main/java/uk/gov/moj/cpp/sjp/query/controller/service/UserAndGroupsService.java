package uk.gov.moj.cpp.sjp.query.controller.service;

import static uk.gov.justice.services.core.annotation.Component.QUERY_CONTROLLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class UserAndGroupsService {

    @Inject
    @ServiceComponent(QUERY_CONTROLLER)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    private static final String PROSECUTOR_GROUP = "TFL Users";

    public boolean isSjpProsecutor(JsonEnvelope originalEnvelope) {

        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(originalEnvelope, "usersgroups.get-groups-by-user")
                .apply(Json.createObjectBuilder().add("userId", originalEnvelope.metadata().userId().get()).build());

        final JsonEnvelope responseEnvelope = requester.requestAsAdmin(requestEnvelope);

        final JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();
        final JsonArray groups = responsePayload.getJsonArray("groups");

        return groups.getValuesAs(JsonObject.class).stream().anyMatch(group -> group.getString("groupName").equals(PROSECUTOR_GROUP));
    }
}
