package uk.gov.moj.cpp.sjp.event.processor.service;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
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
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseFileServiceTest {

    private static final UUID CASE_ID = randomUUID();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ProsecutionCaseFileService prosecutionCaseFileService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Test
    public void shouldReturnCaseFileDefendantDetailsWhenPresent() {
        JsonObject defendant = createObjectBuilder().add("id", "12313").build();
        final JsonObject responsePayload = createObjectBuilder().add(
                "defendants",
                createArrayBuilder().add(defendant)).build();
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), responsePayload);

        when(requestProsecutionCasefileCase()).thenReturn(queryResponse);

        final Optional<JsonObject> result = prosecutionCaseFileService.getCaseFileDefendantDetails(CASE_ID, envelope);
        assertThat(result, is(Optional.of(defendant)));
    }

    @Test
    public void shouldReturnEmptyOptionalWhenCaseFileDetailsNotPresent() {
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);

        when(requestProsecutionCasefileCase()).thenReturn(queryResponse);

        final Optional<JsonObject> result = prosecutionCaseFileService.getCaseFileDefendantDetails(CASE_ID, envelope);
        assertThat(result, is(Optional.empty()));
    }

    private JsonEnvelope requestProsecutionCasefileCase() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("prosecutioncasefile.query.case"),
                payloadIsJson(withJsonPath("$.caseId", equalTo(CASE_ID.toString()))))));
    }
}
