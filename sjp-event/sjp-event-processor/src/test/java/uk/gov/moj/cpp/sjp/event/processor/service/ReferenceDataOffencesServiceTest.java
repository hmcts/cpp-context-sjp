package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataOffencesServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataOffencesService referenceDataOffencesService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    private static final String OFFENCE_CJS_CODE1 = "cjsCode1";
    private static final String OFFENCE_CJS_CODE2 = "cjsCode2";
    private static final UUID OFFENCE_DEFINITION_ID1 = randomUUID();
    private static final UUID OFFENCE_DEFINITION_ID2 = randomUUID();
    private static final String MAX_PENALTY1 = "max penalty1";
    private static final String MAX_PENALTY2 = "max penalty2";
    private static final ZonedDateTime DECISION_DATE = ZonedDateTime.now();

    private static final JsonObject OFFENCE_DEFINITION_1 = buildOffenceJsonObject(OFFENCE_CJS_CODE1, OFFENCE_DEFINITION_ID1, MAX_PENALTY1);
    private static final JsonObject OFFENCE_DEFINITION_2 = buildOffenceJsonObject(OFFENCE_CJS_CODE2, OFFENCE_DEFINITION_ID2, MAX_PENALTY2);

    @Test
    public void shouldReturnOffenceDefinitions() {
        JsonObject responsePayload1 = buildResponsePayload(OFFENCE_DEFINITION_1);
        JsonObject responsePayload2 = buildResponsePayload(OFFENCE_DEFINITION_2);
        when(requestOffenceDefinitions(OFFENCE_CJS_CODE1, DECISION_DATE.toLocalDate())).thenReturn(expectedQueryResponse(responsePayload1));
        when(requestOffenceDefinitions(OFFENCE_CJS_CODE2, DECISION_DATE.toLocalDate())).thenReturn(expectedQueryResponse(responsePayload2));

        final Map<String, JsonObject> result = referenceDataOffencesService.getOffenceDefinitionByOffenceCode(mockOffenceCodes(), DECISION_DATE.toLocalDate(), envelope);
        assertThat(result, is(mockCJSOffenceCodeToOffenceDefinition()));
    }

   @Test
    public void shouldReturnOffenceDefinitionId() {
        final UUID offenceDefinitionId = referenceDataOffencesService.getOffenceDefinitionId(OFFENCE_DEFINITION_1);
        assertThat(offenceDefinitionId, is(OFFENCE_DEFINITION_ID1));
    }

    @Test
    public void shouldReturnMaxPenalty() {
        final String maxPenalty = referenceDataOffencesService.getMaxPenalty(OFFENCE_DEFINITION_1);
        assertThat(maxPenalty, is(MAX_PENALTY1));
    }

    private Object requestOffenceDefinitions(final String cjsCode, final LocalDate referredAt) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedataoffences.query.offences-list"),
                payloadIsJson(allOf(withJsonPath("$.cjsoffencecode", equalTo(cjsCode)),
                        withJsonPath("$.date", equalTo(referredAt.toString())))))));
    }

    private JsonEnvelope expectedQueryResponse(JsonObject responsePayload) {
        return envelopeFrom(
                metadataWithRandomUUID("referencedataoffences.offences-list"),
                responsePayload);
    }

    private static JsonObject buildResponsePayload(JsonObject offenceJsonObject){
        return createObjectBuilder().add("offences", createArrayBuilder()
                .add(offenceJsonObject)).build();
    }

    private static JsonObject buildOffenceJsonObject(String offenceCode, UUID offenceDefinitionId, String maxPenalty){
        return createObjectBuilder()
                        .add("cjsoffencecode", offenceCode)
                        .add("offenceId", offenceDefinitionId.toString())
                        .add("maxPenalty", maxPenalty
                        ).build();
    }

    private static Map<String, JsonObject> mockCJSOffenceCodeToOffenceDefinition() {
        return ImmutableMap.of(OFFENCE_CJS_CODE1, OFFENCE_DEFINITION_1, OFFENCE_CJS_CODE2, OFFENCE_DEFINITION_2);
    }

    private static Set<String> mockOffenceCodes() {
        return Sets.newHashSet(OFFENCE_CJS_CODE1, OFFENCE_CJS_CODE2);
    }
}
