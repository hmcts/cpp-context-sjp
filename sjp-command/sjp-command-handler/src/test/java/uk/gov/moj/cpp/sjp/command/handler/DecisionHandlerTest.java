package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationDecision.applicationDecision;
import static uk.gov.justice.json.schemas.domains.sjp.commands.SaveApplicationDecision.saveApplicationDecision;
import static uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved.applicationDecisionSaved;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty.createFinancialPenalty;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.domains.sjp.commands.SaveApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionRejected;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private Session session;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            DecisionSaved.class,
            ApplicationDecisionSaved.class,
            ApplicationDecisionRejected.class);

    @InjectMocks
    private DecisionHandler decisionHandler;

    private void mockCalls(final UUID caseId, final UUID sessionId) {
        when(eventSource.getStreamById(sessionId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Session.class)).thenReturn(session);
        when(session.getSessionType()).thenReturn(SessionType.MAGISTRATE);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
    }

    @Test
    public void shouldSaveDecision() throws EventStreamException {

        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final UUID caseId = randomUUID();
        final String urn = "TFL1244567";
        final UUID userId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID withdrawalReasonId1 = randomUUID();
        final UUID withdrawalReasonId2 = randomUUID();

        final ZonedDateTime savedAt = ZonedDateTime.now();
        final User savedBy = new User("John", "Smith", userId);

        final List<OffenceDecision> offenceDecisions = new ArrayList<>();
        final OffenceDecision offenceDecision1 = new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, VerdictType.NO_VERDICT), withdrawalReasonId1);
        final OffenceDecision offenceDecision2 = new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId2, VerdictType.NO_VERDICT), withdrawalReasonId2);
        offenceDecisions.add(offenceDecision1);
        offenceDecisions.add(offenceDecision2);

        final Decision decision = new Decision(decisionId, sessionId, caseId, "duplicate conviction", savedAt, savedBy, offenceDecisions, null);

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, urn, decision.getSavedAt(), offenceDecisions);

        when(eventSource.getStreamById(sessionId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Session.class)).thenReturn(session);
        when(session.getSessionType()).thenReturn(SessionType.MAGISTRATE);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        when(session.getSessionType()).thenReturn(SessionType.MAGISTRATE);
        when(caseAggregate.saveDecision(decision, session)).thenReturn(Stream.of(decisionSaved));
        final Envelope<Decision> decisionEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.command.save-decision"), decision);

        decisionHandler.saveDecision(decisionEnvelope);

        verify(eventStream).append(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(decisionEnvelope.metadata(), NULL))
                                .withName("sjp.events.decision-saved"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.urn", equalTo(urn)),
                                withJsonPath("$.decisionId", equalTo(decisionId.toString())),
                                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                withJsonPath("$.savedAt", equalTo(decision.getSavedAt().format(new DateTimeFormatterBuilder()
                                        .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                                        .toFormatter()) + "Z")),
                                withJsonPath("$.offenceDecisions", hasSize(2)),
                                withJsonPath("$.offenceDecisions[0].offenceDecisionInformation.offenceId", equalTo(offenceId1.toString())),
                                withJsonPath("$.offenceDecisions[0].type", equalTo("WITHDRAW")),
                                withJsonPath("$.offenceDecisions[0].withdrawalReasonId", equalTo(withdrawalReasonId1.toString())),
                                withJsonPath("$.offenceDecisions[1].offenceDecisionInformation.offenceId", equalTo(offenceId2.toString())),
                                withJsonPath("$.offenceDecisions[1].type", equalTo("WITHDRAW")),
                                withJsonPath("$.offenceDecisions[1].withdrawalReasonId", equalTo(withdrawalReasonId2.toString()))
                        ))))));
    }

    @Test
    public void shouldSaveDismissDecision() throws EventStreamException {

        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final UUID caseId = randomUUID();
        final String urn = "TFL1244567";
        final UUID userId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final ZonedDateTime savedAt = ZonedDateTime.now();
        final User savedBy = new User("John", "Smith", userId);

        final List<OffenceDecision> offenceDecisions = new ArrayList<>();
        final OffenceDecision offenceDecision1 = new Dismiss(offenceId1, createOffenceDecisionInformation(offenceId1, VerdictType.FOUND_NOT_GUILTY));
        final OffenceDecision offenceDecision2 = new Dismiss(offenceId2, createOffenceDecisionInformation(offenceId2, VerdictType.FOUND_NOT_GUILTY));
        offenceDecisions.add(offenceDecision1);
        offenceDecisions.add(offenceDecision2);

        final Decision decision = new Decision(decisionId, sessionId, caseId, "duplicate conviction", savedAt, savedBy, offenceDecisions, null);

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, urn, decision.getSavedAt(), offenceDecisions);

        when(eventSource.getStreamById(sessionId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Session.class)).thenReturn(session);
        when(session.getSessionType()).thenReturn(SessionType.MAGISTRATE);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(session.getSessionType()).thenReturn(SessionType.MAGISTRATE);
        when(caseAggregate.saveDecision(decision, session)).thenReturn(Stream.of(decisionSaved));
        final Envelope<Decision> decisionEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.command.save-decision"), decision);

        decisionHandler.saveDecision(decisionEnvelope);

        verify(eventStream).append(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(decisionEnvelope.metadata(), NULL))
                                .withName("sjp.events.decision-saved"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.urn", equalTo(urn)),
                                withJsonPath("$.decisionId", equalTo(decisionId.toString())),
                                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                withJsonPath("$.savedAt", equalTo(decision.getSavedAt().format(new DateTimeFormatterBuilder()
                                        .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                                        .toFormatter()) + "Z")),
                                withJsonPath("$.offenceDecisions", hasSize(2)),
                                withJsonPath("$.offenceDecisions[0].offenceDecisionInformation.offenceId", equalTo(offenceId1.toString())),
                                withJsonPath("$.offenceDecisions[0].type", equalTo("DISMISS")),
                                withJsonPath("$.offenceDecisions[1].offenceDecisionInformation.offenceId", equalTo(offenceId2.toString())),
                                withJsonPath("$.offenceDecisions[1].type", equalTo("DISMISS"))
                        ))))));
    }

    @Test
    public void shouldSaveOffencesDecisionWithBackDutyFields() throws EventStreamException {

        final UUID decisionId = randomUUID();
        final UUID sessionId = randomUUID();
        final UUID caseId = randomUUID();
        final String urn = "TFL1244567";
        final UUID userId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final ZonedDateTime savedAt = ZonedDateTime.now();
        final User savedBy = new User("Ruve", "Vem", userId);

        final List<OffenceDecision> offenceDecisions = new ArrayList<>();

        final OffenceDecision offenceDecision1 = createFinancialPenalty(offenceId1, createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), new BigDecimal(10),
                new BigDecimal(20), null, true, new BigDecimal(3), new BigDecimal(4), null);
        final OffenceDecision offenceDecision2 = createDischarge(offenceId2, createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE,
                null, new BigDecimal(10), null, true, new BigDecimal(3), null);
        offenceDecisions.add(offenceDecision1);
        offenceDecisions.add(offenceDecision2);

        final Decision decision = new Decision(decisionId, sessionId, caseId, "duplicate conviction", savedAt, savedBy, offenceDecisions, null);

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, sessionId, caseId, urn, decision.getSavedAt(), offenceDecisions);

        mockCalls(caseId, sessionId);
        when(caseAggregate.saveDecision(decision, session)).thenReturn(Stream.of(decisionSaved));
        final Envelope<Decision> decisionEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.command.save-decision"), decision);

        decisionHandler.saveDecision(decisionEnvelope);

        verify(eventStream).append(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(decisionEnvelope.metadata(), NULL))
                                .withName("sjp.events.decision-saved"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.urn", equalTo(urn)),
                                withJsonPath("$.decisionId", equalTo(decisionId.toString())),
                                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                withJsonPath("$.savedAt", equalTo(decision.getSavedAt().format(new DateTimeFormatterBuilder()
                                        .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                                        .toFormatter()) + "Z")),
                                withJsonPath("$.offenceDecisions", hasSize(2)),
                                withJsonPath("$.offenceDecisions[0].offenceDecisionInformation.offenceId", equalTo(offenceId1.toString())),
                                withJsonPath("$.offenceDecisions[0].type", equalTo("FINANCIAL_PENALTY")),
                                withJsonPath("$.offenceDecisions[0].excisePenalty", equalTo(4)),
                                withJsonPath("$.offenceDecisions[1].offenceDecisionInformation.offenceId", equalTo(offenceId2.toString())),
                                withJsonPath("$.offenceDecisions[1].type", equalTo("DISCHARGE")),
                                withJsonPath("$.offenceDecisions[1].backDuty", equalTo(3)
                                )
                        ))))));
    }

    @Test
    public void shouldSaveApplicationDecision() throws EventStreamException {
        final UUID userId = randomUUID();
        final User savedBy = new User("John", "Smith", userId);
        final UUID applicationId = randomUUID();
        final UUID sessionId = randomUUID();
        final UUID caseId = randomUUID();
        final SaveApplicationDecision applicationDecision = saveApplicationDecision()
                .withApplicationId(applicationId)
                .withCaseId(caseId)
                .withSessionId(sessionId)
                .withGranted(true)
                .withOutOfTime(false)
                .withSavedBy(savedBy)
                .build();

        final ApplicationDecisionSaved applicationDecisionSaved = applicationDecisionSaved()
                .withCaseId(caseId)
                .withApplicationId(applicationId)
                .withSessionId(sessionId)
                .withDecisionId(randomUUID())
                .withSavedAt(ZonedDateTime.now())
                .withApplicationDecision(applicationDecision()
                        .withGranted(true)
                        .withOutOfTime(false)
                        .build())
                .withSavedBy(savedBy)
                .build();

        mockCalls(caseId, sessionId);
        when(caseAggregate.saveApplicationDecision(applicationDecision, session))
                .thenReturn(Stream.of(applicationDecisionSaved));

        final Envelope<SaveApplicationDecision> envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.command.handler.save-application-decision"),
                applicationDecision);

        decisionHandler.saveApplicationDecision(envelope);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(envelope.metadata(), NULL))
                                .withName("sjp.events.application-decision-saved"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.decisionId", notNullValue()),
                                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                withJsonPath("$.savedAt", notNullValue()),
                                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.savedBy.userId", equalTo(savedBy.getUserId().toString())),
                                withJsonPath("$.savedBy.firstName", equalTo(savedBy.getFirstName())),
                                withJsonPath("$.savedBy.lastName", equalTo(savedBy.getLastName())),
                                withJsonPath("$.applicationDecision.granted", equalTo(true)),
                                withJsonPath("$.applicationDecision.outOfTime", equalTo(false))
                        ))))));
    }
}
