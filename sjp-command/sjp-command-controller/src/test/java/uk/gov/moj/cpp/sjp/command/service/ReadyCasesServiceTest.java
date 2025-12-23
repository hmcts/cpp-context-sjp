package uk.gov.moj.cpp.sjp.command.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
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
public class ReadyCasesServiceTest {

    private static final String READY_CASES_QUERY_NAME = "sjp.query.ready-cases";

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private ReadyCasesService readyCasesService;

    private final UUID userId = randomUUID();

    private final JsonEnvelope originalEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().withUserId(userId.toString()), NULL);

    @Test
    public void shouldGetReadyCases() {
        final JsonObject expectedReadyCasesDetails = createObjectBuilder()
                .add("readyCases", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("caseId", randomUUID().toString())
                                .add("reason", "PIA")
                                .add("assigneeId", userId.toString()))
                        .add(createObjectBuilder()
                                .add("caseId", randomUUID().toString())
                                .add("reason", "WITHDRAWAL_REQUESTED")
                                .add("assigneeId", userId.toString()))
                )
                .build();

        when(requester.requestAsAdmin(argThat(readyCasesQuery(userId, originalEnvelope)))).
                thenReturn(envelopeFrom(metadataWithRandomUUID(READY_CASES_QUERY_NAME), expectedReadyCasesDetails));

        final JsonObject readyCasesDetails = readyCasesService.getReadyCasesAssignedToUser(userId, originalEnvelope);

        assertThat(readyCasesDetails, equalTo(expectedReadyCasesDetails));
    }

    private JsonEnvelopeMatcher readyCasesQuery(final UUID userId, final JsonEnvelope envelope) {
        return jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(READY_CASES_QUERY_NAME),
                payloadIsJson(withJsonPath("assigneeId", equalTo(userId.toString()))));
    }
}
