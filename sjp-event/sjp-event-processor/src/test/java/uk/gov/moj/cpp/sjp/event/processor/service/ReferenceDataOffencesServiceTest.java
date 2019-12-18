package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataOffencesServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataOffencesService referenceDataOffencesService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    private static final String OFFENCE_CJS_CODE1 = "cjsCode";
    private static final String OFFENCE_CJS_CODE2 = "cjsCode1";
    private static final UUID OFFENCE_DEFINITION_ID1 = randomUUID();
    private static final UUID OFFENCE_DEFINITION_ID2 = randomUUID();
    private static final ZonedDateTime DECISION_DATE = ZonedDateTime.now();

    @Test
    public void shouldReturnOffenceDefinitionIds() {
        when(requestOffenceDefinitionIds(OFFENCE_CJS_CODE1, DECISION_DATE.toLocalDate())).thenReturn(expectedQueryResponse(OFFENCE_CJS_CODE1, OFFENCE_DEFINITION_ID1));
        when(requestOffenceDefinitionIds(OFFENCE_CJS_CODE2, DECISION_DATE.toLocalDate())).thenReturn(expectedQueryResponse(OFFENCE_CJS_CODE2, OFFENCE_DEFINITION_ID2));

        final Map<String, UUID> offenceDefinitionIdByOffenceCode = referenceDataOffencesService.getOffenceDefinitionIdByOffenceCode(mockOffenceCodes(), DECISION_DATE.toLocalDate(), envelope);
        assertThat(offenceDefinitionIdByOffenceCode, is(mockCJSOffenceCodeToOffenceDefinitionId()));
    }

    private Object requestOffenceDefinitionIds(final String cjsCode, final LocalDate referredAt) {
        return requester.request(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedataoffences.query.offences-list"),
                payloadIsJson(allOf(withJsonPath("$.cjsoffencecode", equalTo(cjsCode)),
                        withJsonPath("$.date", equalTo(referredAt.toString())))))));
    }

    private JsonEnvelope expectedQueryResponse(String offenceCode, UUID offenceDefinitionId) {
        final JsonObject responsePayload = createObjectBuilder().add("offences", createArrayBuilder()
                .add(createObjectBuilder()
                        .add("cjsoffencecode", offenceCode)
                        .add("offenceId", offenceDefinitionId.toString()
                        ))).build();
        return envelopeFrom(
                metadataWithRandomUUID("referencedataoffences.offences-list"),
                responsePayload);
    }

    private static Map<String, UUID> mockCJSOffenceCodeToOffenceDefinitionId() {
        return ImmutableMap.of(OFFENCE_CJS_CODE1, OFFENCE_DEFINITION_ID1, OFFENCE_CJS_CODE2, OFFENCE_DEFINITION_ID2);
    }

    private static Set<String> mockOffenceCodes() {
        return Sets.newHashSet(OFFENCE_CJS_CODE1, OFFENCE_CJS_CODE2);
    }
}
