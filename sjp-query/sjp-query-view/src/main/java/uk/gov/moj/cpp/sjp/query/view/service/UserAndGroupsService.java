package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class UserAndGroupsService {

    private static final List<String> SHOW_ONLINE_PLEA_FINANCES = asList("Legal Advisers", "Court Administrators", "Magistrates");

    private static final String GROUP_SJP_PROSECUTORS = "SJP Prosecutors";

    private static final String SPACE = "";

    private static final String GET_USER_DETAILS= "usersgroups.get-user-details";

    private static final String FIRST_NAME = "firstName";

    private static final String LAST_NAME ="lastName";

    private static final String USER_ID = "userId";
    private static final String GROUP_SECOND_LINE_SUPPORT = "Second Line Support";

    @Inject
    private UserAndGroupProvider userAndGroupProvider;

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    public boolean canSeeOnlinePleaFinances(final JsonEnvelope originalEnvelope) {
        return userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(new Action(originalEnvelope), SHOW_ONLINE_PLEA_FINANCES);
    }

    public boolean isUserProsecutor(final JsonEnvelope originalEnvelope) {
        return userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(new Action(originalEnvelope), GROUP_SJP_PROSECUTORS);
    }

    public boolean isUserSecondLineSupport(final JsonEnvelope originalEnvelope) {
        return userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(new Action(originalEnvelope), GROUP_SECOND_LINE_SUPPORT);
    }

    public String getUserDetails(final UUID userId) {
        final JsonEnvelope usergroupsQueryEnvelope = envelopeFrom(
                metadataBuilder().withName(GET_USER_DETAILS).withId(randomUUID()),
                createObjectBuilder().add(USER_ID, userId.toString()));

        final JsonObject userDetailsPayload = requester.requestAsAdmin(usergroupsQueryEnvelope).payloadAsJsonObject();
        return userDetailsPayload.getString(FIRST_NAME) + " " + userDetailsPayload.getString(LAST_NAME);
    }

    public String getUserDetails(final UUID userId, final JsonEnvelope envelope) {

        final Metadata metadata = metadataFrom(envelope.metadata()).withName(GET_USER_DETAILS).build();
        final Envelope requestEnvelope = envelopeFrom(metadata, createObjectBuilder().add(USER_ID, userId.toString()).build());
        final Envelope<JsonObject> jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        final JsonObject userObject = jsonResultEnvelope.payload();
        String userDetails;
        if (Objects.nonNull(userObject)) {
            userDetails = userObject.getString(FIRST_NAME, SPACE) + " " + userObject.getString(LAST_NAME, SPACE);
        } else {
            userDetails = SPACE;
        }
        return userDetails;
    }
}
