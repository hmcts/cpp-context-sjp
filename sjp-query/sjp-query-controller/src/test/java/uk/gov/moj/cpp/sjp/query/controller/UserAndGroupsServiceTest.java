package uk.gov.moj.cpp.sjp.query.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.controller.service.UserAndGroupsService;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserAndGroupsServiceTest {

    @InjectMocks
    private UserAndGroupsService service;

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Test
    public void isSjpProsecutorWhenUserIsTflUser() {
        final JsonObject groupsPayload = createObjectBuilder()
                .add("groups", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                        .add("groupId", UUID.randomUUID().toString())
                        .add("groupName", "TFL Users"))).build();

        final UUID userId = UUID.randomUUID();
        final JsonEnvelope originalEnvelope = envelopeFrom(
            metadataWithRandomUUID("usersgroups.get-groups-by-user").withUserId(userId.toString()),
            createObjectBuilder().add("userId", userId.toString()).build()
        );

        final JsonEnvelope userAndGroupsResponse = enveloper.withMetadataFrom(originalEnvelope, "usersgroups.get-groups-by-user").apply(groupsPayload);
        when(requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(originalEnvelope).withName("usersgroups.get-groups-by-user"),
                payloadIsJson(withJsonPath("$.userId", is(userId.toString())))
        )))).thenReturn(userAndGroupsResponse);

        final boolean isSjpProsecutor = service.isSjpProsecutor(originalEnvelope);
        assertTrue(isSjpProsecutor);
    }

    @Test
    public void isSjpProsecutorWhenUserIsNotUser() {
        final JsonObject groupsPayload = createObjectBuilder()
                .add("groups", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("groupId", UUID.randomUUID().toString())
                                .add("groupName", "Court Administrators"))).build();

        final UUID userId = UUID.randomUUID();
        final JsonEnvelope originalEnvelope = envelopeFrom(
                metadataWithRandomUUID("usersgroups.get-groups-by-user").withUserId(userId.toString()),
                createObjectBuilder().add("userId", userId.toString()).build()
        );

        final JsonEnvelope userAndGroupsResponse = enveloper.withMetadataFrom(originalEnvelope, "usersgroups.get-groups-by-user").apply(groupsPayload);
        when(requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(originalEnvelope).withName("usersgroups.get-groups-by-user"),
                payloadIsJson(withJsonPath("$.userId", is(userId.toString())))
        )))).thenReturn(userAndGroupsResponse);

        final boolean isSjpProsecutor = service.isSjpProsecutor(originalEnvelope);
        assertFalse(isSjpProsecutor);
    }
}