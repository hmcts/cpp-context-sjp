package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpServiceTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private SjpService sjpService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();

    @Test
    public void shouldGetCaseDetails() {
        final CaseDetails responseCaseDetails = CaseDetails.caseDetails()
                .withId(CASE_ID)
                .build();
        final Envelope<CaseDetails> responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.query.case").build(),
                responseCaseDetails);
        when(requestCaseDetails()).thenReturn(responseEnvelope);

        final CaseDetails result = sjpService.getCaseDetails(CASE_ID, envelope);
        assertThat(result, is(responseCaseDetails));
    }

    @Test
    public void shouldGetDefendantEmployerDetails() {
        final UUID defendantId = randomUUID();

        final EmployerDetails employer = EmployerDetails.employerDetails()
                .withName("employerName")
                .build();
        final Envelope<EmployerDetails> employerEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.query.employer").build(),
                employer);
        when(requestEmployerDetails(defendantId)).thenReturn(employerEnvelope);

        final EmployerDetails result = sjpService.getEmployerDetails(defendantId, envelope);

        assertThat(result, is(employer));
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
                .withDefendantId(DEFENDANT_ID)
                .build();
        final Envelope<DefendantsOnlinePlea> responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.query.defendants-online-plea").build(),
                pleaDetails);
        when(requestOnlinePleaDetails()).thenReturn(responseEnvelope);

        final DefendantsOnlinePlea result = sjpService.getDefendantPleaDetails(CASE_ID, DEFENDANT_ID, envelope);

        assertThat(result, is(pleaDetails));
    }

    @Test
    public void shouldGetPendingCases() {
        final JsonObject pendingCasesPayload = createObjectBuilder()
                .add("pendingCases", createArrayBuilder())
                .build();
        final JsonEnvelope responseEnvolope = envelopeFrom(
                metadataWithRandomUUID("sjp.query.pending-cases").build(),
                pendingCasesPayload
        );

        when(requestPendingCasesList()).thenReturn(responseEnvolope);

        final List<JsonObject> result = sjpService.getPendingCases(envelope, ExportType.PUBLIC);

        assertThat(result, is(createArrayBuilder().build()));
    }

    private Object requestGetSessionDetails(final String sessionId) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                metadata()
                        .withName("sjp.query.session")
                        .withId(envelope.metadata().id()),
                payloadIsJson(withJsonPath("$.sessionId", equalTo(sessionId))))));
    }

    private Object requestEmployerDetails(final UUID defendantId) {
        return requester.request(
                argThat(
                        jsonEnvelope(
                                metadata()
                                        .withName("sjp.query.employer")
                                        .withId(envelope.metadata().id()),
                                payloadIsJson(withJsonPath("$.defendantId", equalTo(defendantId.toString()))))),
                eq(EmployerDetails.class));
    }

    private Object requestOnlinePleaDetails() {
        return requester.request(
                argThat(
                        jsonEnvelope(
                                metadata()
                                        .withName("sjp.query.defendants-online-plea")
                                        .withId(envelope.metadata().id()),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.defendantId", equalTo(DEFENDANT_ID.toString())))
                                        ))),
                eq(DefendantsOnlinePlea.class));
    }

    private Object requestCaseDetails() {
        return requester.request(
                argThat(
                        jsonEnvelope(
                                metadata()
                                        .withName("sjp.query.case")
                                        .withId(envelope.metadata().id()),
                                payloadIsJson(withJsonPath("$.caseId", equalTo(CASE_ID.toString()))))),
                eq(CaseDetails.class));
    }

    private Object requestPendingCasesList() {
        return requester.request(
                argThat(
                        jsonEnvelope(
                                metadata()
                                        .withName("sjp.query.pending-cases")
                                        .withId(envelope.metadata().id()),
                                payloadIsJson(notNullValue())
                        ))
        );
    }

}
