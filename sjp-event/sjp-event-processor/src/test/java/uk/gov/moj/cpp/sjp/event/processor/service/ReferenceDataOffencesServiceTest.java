package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
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

import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
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
public class ReferenceDataOffencesServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataOffencesService referenceDataOffencesService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Test
    public void shouldReturnOffences() {
        final String cjsCode = "cjsCode";
        final JsonObject responsePayload = createObjectBuilder().add("cjsoffencecode", cjsCode).build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedataoffences.offences-list"),
                responsePayload);

        final Offence offence = Offence.offence()
                .withCjsCode(cjsCode)
                .build();
        when(requestOffences(cjsCode)).thenReturn(queryResponse);

        final JsonObject offences = referenceDataOffencesService.getOffences(offence, envelope);
        assertThat(offences, is(responsePayload));
    }

    @Test
    public void shouldReturnOffence() {
        final UUID offenceId = UUID.randomUUID();
        final JsonObject responsePayload = createObjectBuilder().add("offenceId", offenceId.toString()).build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedataoffences.query.offence"),
                responsePayload);

        final Offence offence = Offence.offence()
                .withId(offenceId)
                .build();
        when(requestOffence(offenceId.toString())).thenReturn(queryResponse);

        final JsonObject offenceObject = referenceDataOffencesService.findOffence(offence, envelope);
        assertThat(offenceObject, is(responsePayload));
    }

    private Object requestOffence(final String offenceId) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedataoffences.query.offence"),
                payloadIsJson(withJsonPath("$.offenceId", equalTo(offenceId))))));
    }

    private Object requestOffences(final String cjsCode) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedataoffences.query.offences-list"),
                payloadIsJson(withJsonPath("$.cjsoffencecode", equalTo(cjsCode))))));
    }


}
