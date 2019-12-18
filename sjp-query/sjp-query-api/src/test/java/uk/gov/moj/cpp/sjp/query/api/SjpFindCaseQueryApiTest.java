package uk.gov.moj.cpp.sjp.query.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.api.decorator.DecisionDecorator;
import uk.gov.moj.cpp.sjp.query.api.decorator.OffenceDecorator;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpFindCaseQueryApiTest {

    private final UUID caseId = randomUUID();

    private JsonEnvelope originalQueryEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.query.case"),
            createObjectBuilder().add("caseId", caseId.toString()).build());

    @Mock
    private Requester requester;

    @Mock
    private OffenceDecorator offenceDecorator;

    @Mock
    private DecisionDecorator decisionDecorator;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @InjectMocks
    private SjpQueryApi sjpQueryApi;

    @Test
    public void shouldFindAndDecorateCase() {
        final JsonObject originalCaseDetails = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();
        final JsonObject caseDetailsDecoratedWithOffences = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("newProperty", "newProperty")
                .build();
        JsonObject caseDetailsDecoratedWithLegalAdviserName = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("newProperty", "newProperty")
                .add("legalAdviserName", "name")
                .build();

        final JsonEnvelope originalCaseResponse = envelopeFrom(metadataWithRandomUUIDAndName(), originalCaseDetails);

        when(requester.request(originalQueryEnvelope)).thenReturn(originalCaseResponse);
        when(offenceDecorator.decorateAllOffences(eq(originalCaseDetails), eq(originalQueryEnvelope), any(WithdrawalReasons.class))).thenReturn(caseDetailsDecoratedWithOffences);
        when(decisionDecorator.decorate(eq(caseDetailsDecoratedWithOffences), eq(originalQueryEnvelope), any(WithdrawalReasons.class))).thenReturn(caseDetailsDecoratedWithLegalAdviserName);

        final JsonEnvelope actualCaseResponse = sjpQueryApi.findCase(originalQueryEnvelope);

        assertThat(actualCaseResponse, jsonEnvelope(withMetadataEnvelopedFrom(originalCaseResponse), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.caseId", equalTo(caseId.toString())),
                withJsonPath("$.newProperty", equalTo("newProperty")),
                withJsonPath("$.legalAdviserName", equalTo("name"))))));

        verify(offenceDecorator).decorateAllOffences(eq(originalCaseDetails), eq(originalQueryEnvelope), any(WithdrawalReasons.class));
    }

    @Test
    public void shouldReturnEmptyCase() {
        final JsonValue originalCaseDetails = JsonValue.NULL;
        final JsonEnvelope originalCaseResponse = envelopeFrom(metadataWithRandomUUIDAndName(), originalCaseDetails);

        when(requester.request(originalQueryEnvelope)).thenReturn(originalCaseResponse);

        final JsonEnvelope actualCaseResponse = sjpQueryApi.findCase(originalQueryEnvelope);

        assertThat(actualCaseResponse, is(originalCaseResponse));

        verify(offenceDecorator, never()).decorateAllOffences(any(), any(), any());
        verify(decisionDecorator, never()).decorate(any(), any(), any());
    }

}
