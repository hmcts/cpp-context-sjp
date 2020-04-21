package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class UserAndGroupsService {

    private static final List<String> SHOW_ONLINE_PLEA_FINANCES = asList("Legal Advisers", "Court Administrators");

    private static final String GROUP_SJP_PROSECUTORS = "SJP Prosecutors";

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

    public String getUserDetails(final UUID userId) {
        final JsonEnvelope usergroupsQueryEnvelope = envelopeFrom(
                metadataBuilder().withName("usersgroups.get-user-details").withId(randomUUID()),
                createObjectBuilder().add("userId", userId.toString()));

        final JsonObject userDetailsPayload = requester.requestAsAdmin(usergroupsQueryEnvelope).payloadAsJsonObject();
        return userDetailsPayload.getString("firstName") + " " + userDetailsPayload.getString("lastName");
    }
}
