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

import java.util.Arrays;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
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

    private JsonEnvelope setupMocksAndStubData(String[] userGroupNames) {
        final JsonArrayBuilder groupsArray = Json.createArrayBuilder();
        Arrays.stream(userGroupNames).forEach(userGroup -> {
            groupsArray.add(createObjectBuilder().add("groupId", UUID.randomUUID().toString())
                    .add("groupName", userGroup));
        });

        final JsonObject groupsPayload = createObjectBuilder()
                .add("groups", groupsArray).build();

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

        return originalEnvelope;
    }

    @Test
    public void isSjpProsecutorWhenUserIsTflUserGroupOnly() {
        String[] userGroupNames = { "TFL Users" };
        final JsonEnvelope originalEnvelope = setupMocksAndStubData(userGroupNames);

        final boolean isSjpProsecutor = service.isSjpProsecutorUserGroupOnly(originalEnvelope);
        assertTrue(isSjpProsecutor);
    }

    @Test
    public void isSjpProsecutorWhenUserIsCourtAdminUserGroupOnly() {
        String[] userGroupNames = { "Court Administrators" };
        final JsonEnvelope originalEnvelope = setupMocksAndStubData(userGroupNames);


        final boolean isSjpProsecutor = service.isSjpProsecutorUserGroupOnly(originalEnvelope);
        assertFalse(isSjpProsecutor);
    }

    @Test
    public void isSjpProsecutorWhenUserIsLegalAdvisorUserGroupOnly() {
        String[] userGroupNames = { "Legal Advisers" };
        final JsonEnvelope originalEnvelope = setupMocksAndStubData(userGroupNames);

        final boolean isSjpProsecutor = service.isSjpProsecutorUserGroupOnly(originalEnvelope);
        assertFalse(isSjpProsecutor);
    }

    @Test
    public void isSjpProsecutorWhenUserHasAllUserGroups() {
        String[] userGroupNames = { "TFL Users", "Court Administrators", "Legal Advisers" };
        final JsonEnvelope originalEnvelope = setupMocksAndStubData(userGroupNames);

        final boolean isSjpProsecutor = service.isSjpProsecutorUserGroupOnly(originalEnvelope);
        assertFalse(isSjpProsecutor);
    }

    @Test
    public void isSjpProsecutorWhenUserHasTflAndCourtAdminUserGroups() {
        String[] userGroupNames = { "TFL Users", "Court Administrators" };
        final JsonEnvelope originalEnvelope = setupMocksAndStubData(userGroupNames);

        final boolean isSjpProsecutor = service.isSjpProsecutorUserGroupOnly(originalEnvelope);
        assertFalse(isSjpProsecutor);
    }

    @Test
    public void isSjpProsecutorWhenUserHasTflAndLegalAdvisorUserGroups() {
        String[] userGroupNames = { "TFL Users", "Legal Advisers" };
        final JsonEnvelope originalEnvelope = setupMocksAndStubData(userGroupNames);

        final boolean isSjpProsecutor = service.isSjpProsecutorUserGroupOnly(originalEnvelope);
        assertFalse(isSjpProsecutor);
    }

    @Test
    public void isSjpProsecutorWhenUserHasCourtAdminAndLegalAdvisorUserGroups() {
        String[] userGroupNames = { "Court Administrators", "Legal Advisers" };
        final JsonEnvelope originalEnvelope = setupMocksAndStubData(userGroupNames);

        final boolean isSjpProsecutor = service.isSjpProsecutorUserGroupOnly(originalEnvelope);
        assertFalse(isSjpProsecutor);
    }
}