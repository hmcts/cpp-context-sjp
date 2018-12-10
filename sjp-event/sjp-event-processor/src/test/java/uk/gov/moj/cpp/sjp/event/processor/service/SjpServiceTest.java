package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
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
public class SjpServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private SjpService sjpService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    private static final UUID CASE_ID = randomUUID();

    @Test
    public void shouldGetCaseDetails() {
        final CaseDetails responseCaseDetails = CaseDetails.caseDetails()
                .withId(CASE_ID)
                .build();
        final Envelope<CaseDetails> responseEnvelope = Envelope.envelopeFrom(
                metadataWithRandomUUID("sjp.query.case").build(),
                responseCaseDetails);
        when(requestCaseDetails()).thenReturn(responseEnvelope);

        final CaseDetails result = sjpService.getCaseDetails(CASE_ID, envelope);
        assertThat(result, is(responseCaseDetails));
    }

    @Test
    public void shouldGetSessionDetails() {
        final UUID sessionId = randomUUID();
        final JsonObject responsePayload = createObjectBuilder().add("sessionId", sessionId.toString()).build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("sjp.query.session"),
                responsePayload);
        when(requestGetSessionDetails(sessionId.toString())).thenReturn(queryResponse);

        final JsonObject sessionDetails = sjpService.getSessionDetails(sessionId, envelope);
        assertThat(sessionDetails, is(responsePayload));
    }

    @Test
    public void shouldGetDefendantOnlinePleaDetails() {
        final DefendantsOnlinePlea pleaDetails = DefendantsOnlinePlea.defendantsOnlinePlea()
                .withCaseId(CASE_ID)
                .build();
        final Envelope<DefendantsOnlinePlea> responseEnvelope = Envelope.envelopeFrom(
                metadataWithRandomUUID("sjp.query.defendants-online-plea").build(),
                pleaDetails);
        when(requestOnlinePleaDetails()).thenReturn(responseEnvelope);

        final DefendantsOnlinePlea result = sjpService.getDefendantPleaDetails(CASE_ID, envelope);

        assertThat(result, is(pleaDetails));
    }

    private Object requestGetSessionDetails(final String sessionId) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("sjp.query.session"),
                payloadIsJson(withJsonPath("$.sessionId", equalTo(sessionId))))));
    }

    private Object requestOnlinePleaDetails() {
        return requester.request(
                argThat(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope).withName("sjp.query.defendants-online-plea"),
                                payloadIsJson(withJsonPath("$.caseId", equalTo(CASE_ID.toString()))))),
                eq(DefendantsOnlinePlea.class));
    }

    private Object requestCaseDetails() {
        return requester.request(
                argThat(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope).withName("sjp.query.case"),
                                payloadIsJson(withJsonPath("$.caseId", equalTo(CASE_ID.toString()))))),
                eq(CaseDetails.class));
    }
}
