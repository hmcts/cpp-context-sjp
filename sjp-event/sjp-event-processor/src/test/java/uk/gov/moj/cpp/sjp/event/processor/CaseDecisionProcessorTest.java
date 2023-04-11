package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.ResultingToResultsConverterHelper.buildCaseDetails;


import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.SjpToHearingConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDecisionProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Mock
    private SjpService sjpService;

    final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

    @Mock
    protected Function function;

    @Mock
    private SjpToHearingConverter sjpToHearingConverter;

    @Mock
    private PublicHearingResulted publicHearingResultedPayload;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @InjectMocks
    private CaseDecisionProcessor caseDecisionProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    private static final String PUBLIC_CASE_DECISION_SAVED_EVENT = "public.sjp.case-decision-saved";
    private static final String PUBLIC_HEARING_RESULTED_EVENT = "public.hearing.resulted";
    private static final String PRIVATE_CASE_DECISION_SAVED_EVENT = "sjp.events.decision-saved";
    public static final String UNDO_RESERVE_CASE_TIMER_COMMAND = "sjp.command.undo-reserve-case";

    @Before
    public void setUp() {
        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(jsonEnvelope);
    }

    @Test
    public void shouldHandleCaseDecisionSaved() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final LocalDate savedAt = LocalDate.now();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID withdrawalReasonId = randomUUID();
        final String type = "WITHDRAW";
        final String verdict = "NO_VERDICT";

        final JsonEnvelope privateEvent = createEnvelope(PRIVATE_CASE_DECISION_SAVED_EVENT,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("decisionId", decisionId.toString())
                        .add("sessionId", sessionId.toString())
                        .add("savedAt", savedAt.toString())
                        .add("offenceDecisions",
                                createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("type", type)
                                                .add("offenceId", offence1Id.toString())
                                                .add("withdrawalReasonId", withdrawalReasonId.toString())
                                                .add("verdict", verdict))
                                        .add(createObjectBuilder()
                                                .add("type", type)
                                                .add("offenceId", offence2Id.toString())
                                                .add("withdrawalReasonId", withdrawalReasonId.toString())
                                                .add("verdict", verdict))
                        ).build());

        when(sjpToHearingConverter.convertCaseDecision(privateEvent)).thenReturn(publicHearingResultedPayload);

        caseDecisionProcessor.handleCaseDecisionSaved(privateEvent);

        verify(sender, times(2)).send(jsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> eventEnvelopes = jsonEnvelopeCaptor.getAllValues();
        final Envelope<JsonValue> decisionSavedPublicEvent = eventEnvelopes.get(0);

        assertThat(decisionSavedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(privateEvent)
                        .withName(PUBLIC_CASE_DECISION_SAVED_EVENT));

        assertThat(decisionSavedPublicEvent.payload(),
                payloadIsJson(allOf(
                        withJsonPath("caseId", is(caseId.toString())),
                        withJsonPath("decisionId", is(decisionId.toString())),
                        withJsonPath("sessionId", is(sessionId.toString())),
                        withJsonPath("savedAt", is(savedAt.toString())),
                        withJsonPath("offenceDecisions[0].type", is(type)),
                        withJsonPath("offenceDecisions[0].offenceId", is(offence1Id.toString())),
                        withJsonPath("offenceDecisions[0].withdrawalReasonId", is(withdrawalReasonId.toString())),
                        withJsonPath("offenceDecisions[0].verdict", is(verdict)),
                        withJsonPath("offenceDecisions[1].type", is(type)),
                        withJsonPath("offenceDecisions[1].offenceId", is(offence2Id.toString())),
                        withJsonPath("offenceDecisions[1].withdrawalReasonId", is(withdrawalReasonId.toString())),
                        withJsonPath("offenceDecisions[1].verdict", is(verdict))
                )));

        final Envelope<JsonValue> hearingResultedPublicEvent = eventEnvelopes.get(1);

        assertThat(hearingResultedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(privateEvent)
                        .withName(PUBLIC_HEARING_RESULTED_EVENT));

        assertThat(hearingResultedPublicEvent.payload(), is(publicHearingResultedPayload));

        verify(sender, times(1)).sendAsAdmin(envelopeCaptor.capture());

        final Envelope<JsonValue> reserveCaseCommand = envelopeCaptor.getValue();
        assertThat(reserveCaseCommand.metadata().name(), is(UNDO_RESERVE_CASE_TIMER_COMMAND));
        assertThat(reserveCaseCommand.payload(),
                payloadIsJson(allOf(
                        withJsonPath("caseId", is(caseId.toString()))
                )));
    }

    @Test
    public void shouldCallSetPleasWhenCaseDecisionSavedIsSetAside() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final LocalDate savedAt = LocalDate.now();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID withdrawalReasonId = randomUUID();
        final String type = "SET_ASIDE";
        final String verdict = "NO_VERDICT";

        final JsonEnvelope privateEvent = createEnvelope(PRIVATE_CASE_DECISION_SAVED_EVENT,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("decisionId", decisionId.toString())
                        .add("sessionId", sessionId.toString())
                        .add("savedAt", savedAt.toString())
                        .add("offenceDecisions",
                                createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("type", type)
                                                .add("offenceId", offence1Id.toString()))
                                        .add(createObjectBuilder()
                                                .add("type", type)
                                                .add("offenceId", offence2Id.toString()))
                        ).build());

        when(sjpToHearingConverter.convertCaseDecision(privateEvent)).thenReturn(publicHearingResultedPayload);

        caseDecisionProcessor.handleCaseDecisionSaved(privateEvent);

        verify(sender, times(3)).send(jsonEnvelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = jsonEnvelopeCaptor.getAllValues().get(0);

        assertThat(sentEnvelope, is(jsonEnvelope));

    }

    @Test
    public void handleCaseDecisionSaved_shouldNotInitiatePublicHearingResultedEvent() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final LocalDate savedAt = LocalDate.now();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID withdrawalReasonId = randomUUID();
        final String type = "REFER_FOR_COURT_HEARING";
        final String verdict = "NO_VERDICT";

        final JsonEnvelope privateEvent = createEnvelope(PRIVATE_CASE_DECISION_SAVED_EVENT,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("decisionId", decisionId.toString())
                        .add("sessionId", sessionId.toString())
                        .add("savedAt", savedAt.toString())
                        .add("offenceDecisions",
                                createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("type", type)
                                                .add("offenceId", offence1Id.toString())
                                                .add("withdrawalReasonId", withdrawalReasonId.toString())
                                                .add("verdict", verdict))
                                        .add(createObjectBuilder()
                                                .add("type", type)
                                                .add("offenceId", offence2Id.toString())
                                                .add("withdrawalReasonId", withdrawalReasonId.toString())
                                                .add("verdict", verdict))
                        ).build());

        when(sjpToHearingConverter.convertCaseDecision(privateEvent)).thenReturn(publicHearingResultedPayload);

        caseDecisionProcessor.handleCaseDecisionSaved(privateEvent);

        verify(sender, times(0)).send(jsonEnvelopeCaptor.capture());


    }

}
