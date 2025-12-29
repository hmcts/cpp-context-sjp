package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
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
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.SjpToHearingConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDecisionProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Mock
    private SjpService sjpService;

    final DefaultEnvelope jsonEnvelope = mock(DefaultEnvelope.class);

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
    private ArgumentCaptor<DefaultEnvelope> jsonEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    private static final String PUBLIC_CASE_DECISION_SAVED_EVENT = "public.sjp.case-decision-saved";
    private static final String PUBLIC_CASE_DECISION_REFERRED_TO_COURT_EVENT = "public.events.sjp.case-referred-to-court";
    private static final String PUBLIC_HEARING_RESULTED_EVENT = "public.hearing.resulted";
    private static final String PUBLIC_EVENTS_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    public static final String SJP_COMMAND_SAVE_APPLICATION_OFFENCES_RESULTS = "sjp.command.save-application-offences-results";
    private static final String PRIVATE_CASE_DECISION_SAVED_EVENT = "sjp.events.decision-saved";
    public static final String UNDO_RESERVE_CASE_TIMER_COMMAND = "sjp.command.undo-reserve-case";

    @Test
    public void shouldHandleCaseDecisionSaved() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final UUID defendantId = randomUUID();
        final String urn = "TFL12345567";
        final String defendantName = "James Smith";
        final LocalDate savedAt = LocalDate.now();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID withdrawalReasonId = randomUUID();
        final String type = "WITHDRAW";
        final String verdict = "NO_VERDICT";

        final JsonEnvelope privateEvent = createEnvelope(PRIVATE_CASE_DECISION_SAVED_EVENT,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("urn", urn)
                        .add("decisionId", decisionId.toString())
                        .add("sessionId", sessionId.toString())
                        .add("savedAt", savedAt.toString())
                        .add("defendantId", defendantId.toString())
                        .add("defendantName", defendantName)
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
        when(publicHearingResultedPayload.getSharedTime()).thenReturn(ZonedDateTime.now());

        caseDecisionProcessor.handleCaseDecisionSaved(privateEvent);

        verify(sender, times(2)).send(jsonEnvelopeCaptor.capture());

        final List<DefaultEnvelope> eventEnvelopes = jsonEnvelopeCaptor.getAllValues();
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

        final Envelope hearingResultedPublicEvent = eventEnvelopes.get(1);

        assertThat(hearingResultedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(privateEvent)
                        .withName(PUBLIC_EVENTS_HEARING_RESULTED));

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
        when(sjpService.getCaseDetails(any(), any())).thenReturn(buildCaseDetails());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(jsonEnvelope);
        final UUID caseId = randomUUID();
        final String urn = "TFL12345567";
        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final LocalDate savedAt = LocalDate.now();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final String type = "SET_ASIDE";

        final JsonEnvelope privateEvent = createEnvelope(PRIVATE_CASE_DECISION_SAVED_EVENT,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("urn", urn)
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
        when(publicHearingResultedPayload.getSharedTime()).thenReturn(ZonedDateTime.now());

        caseDecisionProcessor.handleCaseDecisionSaved(privateEvent);

        verify(sender, times(3)).send(jsonEnvelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = jsonEnvelopeCaptor.getAllValues().get(0);

        assertThat(sentEnvelope, is(jsonEnvelope));

    }

    @Test
    public void handleCaseDecisionSaved_shouldInitiatePublicCaseReferredToCourtEvent() {
        final UUID caseId = randomUUID();
        final String urn = "TFL12345567";
        final UUID defendantId = randomUUID();
        final String defendantName = "James Smith";
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
                        .add("urn", urn)
                        .add("defendantId", defendantId.toString())
                        .add("defendantName", defendantName)
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

        caseDecisionProcessor.handleCaseDecisionSaved(privateEvent);

        verify(sender, times(1)).send(jsonEnvelopeCaptor.capture());

        final List<DefaultEnvelope> eventEnvelopes = jsonEnvelopeCaptor.getAllValues();
        final Envelope<JsonValue> decisionSavedPublicEvent = eventEnvelopes.get(0);

        assertThat(decisionSavedPublicEvent.metadata(),
                withMetadataEnvelopedFrom(privateEvent)
                        .withName(PUBLIC_CASE_DECISION_REFERRED_TO_COURT_EVENT));

        assertThat(decisionSavedPublicEvent.payload(),
                payloadIsJson(allOf(
                        withJsonPath("caseId", is(caseId.toString())),
                        withJsonPath("urn", is(urn)),
                        withJsonPath("decisionId", is(decisionId.toString())),
                        withJsonPath("sessionId", is(sessionId.toString())),
                        withJsonPath("savedAt", is(savedAt.toString())),
                        withJsonPath("defendantId", is(defendantId.toString())),
                        withJsonPath("defendantName", is(defendantName)),
                        withJsonPath("offenceDecisions[0].type", is(type)),
                        withJsonPath("offenceDecisions[0].offenceId", is(offence1Id.toString())),
                        withJsonPath("offenceDecisions[0].withdrawalReasonId", is(withdrawalReasonId.toString())),
                        withJsonPath("offenceDecisions[0].verdict", is(verdict)),
                        withJsonPath("offenceDecisions[1].type", is(type)),
                        withJsonPath("offenceDecisions[1].offenceId", is(offence2Id.toString())),
                        withJsonPath("offenceDecisions[1].withdrawalReasonId", is(withdrawalReasonId.toString())),
                        withJsonPath("offenceDecisions[1].verdict", is(verdict))
                )));
    }

    @Test
    public void shouldHandleCaseDecisionSavedForApplicationFlow() {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final UUID defendantId = randomUUID();
        final String urn = "TFL12345567";
        final String defendantName = "James Smith";
        final LocalDate savedAt = LocalDate.now();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID withdrawalReasonId = randomUUID();
        final String type = "WITHDRAW";
        final String verdict = "NO_VERDICT";

        final JsonEnvelope privateEvent = createEnvelope(PRIVATE_CASE_DECISION_SAVED_EVENT,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("urn", urn)
                        .add("decisionId", decisionId.toString())
                        .add("sessionId", sessionId.toString())
                        .add("savedAt", savedAt.toString())
                        .add("defendantId", defendantId.toString())
                        .add("defendantName", defendantName)
                        .add("applicationFlow",true)
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
        when(publicHearingResultedPayload.getSharedTime()).thenReturn(ZonedDateTime.now());

        caseDecisionProcessor.handleCaseDecisionSaved(privateEvent);

        verify(sender, times(2)).send(jsonEnvelopeCaptor.capture());

        final List<DefaultEnvelope> eventEnvelopes = jsonEnvelopeCaptor.getAllValues();
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

        final Envelope<JsonValue> hearingResultedCommandSaveApplicationOffenceResults = eventEnvelopes.get(1);

        assertThat(hearingResultedCommandSaveApplicationOffenceResults.metadata(),
                withMetadataEnvelopedFrom(privateEvent)
                        .withName(SJP_COMMAND_SAVE_APPLICATION_OFFENCES_RESULTS));

        verify(sender, times(1)).sendAsAdmin(envelopeCaptor.capture());

        final Envelope<JsonValue> reserveCaseCommand = envelopeCaptor.getValue();
        assertThat(reserveCaseCommand.metadata().name(), is(UNDO_RESERVE_CASE_TIMER_COMMAND));
        assertThat(reserveCaseCommand.payload(),
                payloadIsJson(allOf(
                        withJsonPath("caseId", is(caseId.toString()))
                )));
    }

}
