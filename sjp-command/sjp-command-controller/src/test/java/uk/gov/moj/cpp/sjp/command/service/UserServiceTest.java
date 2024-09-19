package uk.gov.moj.cpp.sjp.command.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
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
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private static final String USER_DETAILS_QUERY_NAME = "usersgroups.get-user-details";

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private UserService userService;

    private final UUID userId = randomUUID();

    private final JsonEnvelope originalEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().withUserId(userId.toString()), NULL);

    @Test
    public void shouldGetUserDetails() {
        final JsonObject expectedUserDetails = createObjectBuilder()
                .add("firstName", "FN")
                .add("lastName", "LN")
                .build();

        when(requester.requestAsAdmin(argThat(userDetailsQuery(userId, originalEnvelope)))).
                thenReturn(envelopeFrom(metadataWithRandomUUID(USER_DETAILS_QUERY_NAME), expectedUserDetails));

        final JsonObject actualUserDetails = userService.getCallingUserDetails(originalEnvelope);

        assertThat(actualUserDetails, equalTo(expectedUserDetails));
    }

    private JsonEnvelopeMatcher userDetailsQuery(final UUID userId, final JsonEnvelope envelope) {
        return jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(USER_DETAILS_QUERY_NAME),
                payloadIsJson(withJsonPath("$.userId", equalTo(userId.toString()))));
    }
}
