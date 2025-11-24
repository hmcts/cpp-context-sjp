package uk.gov.moj.cpp.sjp.query.controller.service;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.QUERY_CONTROLLER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;

public class UserAndGroupsService {

    private static final Logger LOGGER = getLogger(UserAndGroupsService.class);

    @Inject
    @ServiceComponent(QUERY_CONTROLLER)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    private static final String PROSECUTOR_GROUP = "SJP Prosecutors";
    private static final String LEGAL_ADVISER_GROUP = "Legal Advisers";
    private static final String COURT_ADMINISTRATOR_GROUP = "Court Administrators";


    public boolean isSjpProsecutorUserGroupOnly(JsonEnvelope originalEnvelope) {

        final JsonEnvelope requestEnvelope = enveloper.withMetadataFrom(originalEnvelope, "usersgroups.get-groups-by-user")
                .apply(Json.createObjectBuilder().add("userId", originalEnvelope.metadata().userId().get()).build());

        final JsonEnvelope responseEnvelope = requester.requestAsAdmin(requestEnvelope);

        try {
            final JsonObject responsePayload = responseEnvelope.payloadAsJsonObject();
            final JsonArray groups = responsePayload.getJsonArray("groups");

            return groups.getValuesAs(JsonObject.class).stream().anyMatch(group -> group.getString("groupName").equals(PROSECUTOR_GROUP)) &&
                    groups.getValuesAs(JsonObject.class).stream().noneMatch(group ->
                            group.getString("groupName").equals(LEGAL_ADVISER_GROUP) || group.getString("groupName").equals(COURT_ADMINISTRATOR_GROUP)
                    );
        } catch (ClassCastException e) {
            LOGGER.info("Could not cast: {}. Payload is null? {}", e.getMessage(), responseEnvelope.payload() == null, e);
            throw e;
        }
    }
}