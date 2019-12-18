package uk.gov.moj.cpp.sjp.query.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UsersGroupsServiceTest {

    private static final UUID USER_ID = randomUUID();
    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Requester requester;
    @InjectMocks
    private UsersGroupsService usersGroupsService;

    @Test
    public void shouldGetUserDetails() {
        final JsonObject userDetails = createObjectBuilder()
                .add("firstName", "bar")
                .build();
        final JsonEnvelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("usersgroups.get-user-details").build(),
                userDetails);

        when(requestUserDetails()).thenReturn(responseEnvelope);

        final JsonObject result = usersGroupsService.getUserDetails(USER_ID, envelope);

        assertThat(result, is(userDetails));
    }

    private Object requestUserDetails() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("usersgroups.get-user-details"),
                payloadIsJson(withJsonPath("$.userId", equalTo(USER_ID.toString()))))));
    }

}
