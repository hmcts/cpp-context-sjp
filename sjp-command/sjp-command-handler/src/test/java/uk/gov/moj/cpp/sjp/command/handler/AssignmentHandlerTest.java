package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedAfter;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRequested;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentHandlerTest {

    private static final String ASSIGN_CASE_COMMAND = "sjp.command.assign-case";
    private static final String UNASSIGN_CASE_COMMAND = "sjp.command.unassign-case";
    private static final String ASSIGN_CASE__FROM_CANDIDATES_LIST_COMMAND = "sjp.command.assign-case-from-candidates-list";

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream sessionEventStream, case1EventStream, case2EventStream, case3EventStream;

    @Mock
    private Session session;

    @Mock
    private CaseAggregate caseAggregate1, caseAggregate2, caseAggregate3;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(CaseAssigned.class, CaseAssignmentRequested.class);

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @InjectMocks
    private AssignmentHandler assignmentHandler;

    @Test
    public void shouldAssignFirstAssignmentCandidateThatHasTheSameVersionAsCorrespondingCaseEventStream() throws EventStreamException {

        final ZonedDateTime assignedAt = clock.now();
        final UUID sessionId = randomUUID();
        final UUID assigneeId = randomUUID();

        final UUID assignmentCandidate1Id = randomUUID();
        final UUID assignmentCandidate2Id = randomUUID();
        final UUID assignmentCandidate3Id = randomUUID();

        long assignmentCandidate1Version = 10;
        long assignmentCandidate2Version = 9;
        long assignmentCandidate3Version = 8;

        final JsonEnvelope assignCaseFromCandidatesListCommand = envelopeFrom(metadataWithRandomUUID(ASSIGN_CASE__FROM_CANDIDATES_LIST_COMMAND), createObjectBuilder()
                .add("sessionId", sessionId.toString())
                .add("assignmentCandidates", createArrayBuilder().add(
                        createObjectBuilder()
                                .add("caseId", assignmentCandidate1Id.toString())
                                .add("caseStreamVersion", assignmentCandidate1Version))
                        .add(createObjectBuilder()
                                .add("caseId", assignmentCandidate2Id.toString())
                                .add("caseStreamVersion", assignmentCandidate2Version))
                        .add(createObjectBuilder()
                                .add("caseId", assignmentCandidate3Id.toString())
                                .add("caseStreamVersion", assignmentCandidate3Version))
                )
                .build());

        final CaseAssigned caseAssigned = new CaseAssigned(assignmentCandidate2Id, assigneeId, assignedAt, MAGISTRATE_DECISION);

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(aggregateService.get(case1EventStream, CaseAggregate.class)).thenReturn(caseAggregate1);
        when(aggregateService.get(case2EventStream, CaseAggregate.class)).thenReturn(caseAggregate2);
        when(aggregateService.get(case3EventStream, CaseAggregate.class)).thenReturn(caseAggregate3);

        when(session.getId()).thenReturn(sessionId);
        when(session.getUser()).thenReturn(assigneeId);
        when(session.getSessionType()).thenReturn(SessionType.MAGISTRATE);

        when(eventSource.getStreamById(assignmentCandidate1Id)).thenReturn(case1EventStream);
        when(eventSource.getStreamById(assignmentCandidate2Id)).thenReturn(case2EventStream);
        when(case1EventStream.getCurrentVersion()).thenReturn(assignmentCandidate1Version + 1);
        when(case2EventStream.getCurrentVersion()).thenReturn(assignmentCandidate2Version);
        when(caseAggregate2.assignCase(assigneeId, assignedAt, MAGISTRATE_DECISION)).thenReturn(Stream.of(caseAssigned));

        assignmentHandler.assignCaseFromCandidatesList(assignCaseFromCandidatesListCommand);

        verify(case1EventStream, never()).append(any());
        verify(case3EventStream, never()).append(any());
        verify(eventSource, never()).getStreamById(assignmentCandidate3Id);

        assertThat(case2EventStream, eventStreamAppendedAfter(assignmentCandidate2Version).with(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(assignCaseFromCandidatesListCommand)
                                        .withName(CaseAssigned.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(assignmentCandidate2Id.toString())),
                                        withJsonPath("$.assigneeId", equalTo(assigneeId.toString())),
                                        withJsonPath("$.assignedAt", equalTo(assignedAt.toString())),
                                        withJsonPath("$.caseAssignmentType", equalTo(MAGISTRATE_DECISION.toString()))
                                ))))));
    }

    @Test
    public void shouldRequestCaseAssignment() throws EventStreamException {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        final JsonEnvelope assignCaseCommand = envelopeFrom(
                metadataWithRandomUUID(ASSIGN_CASE_COMMAND).withUserId(userId.toString()),
                createObjectBuilder().add("sessionId", sessionId.toString()).build()
        );

        final CaseAssignmentRequested caseAssignmentRequested = new CaseAssignmentRequested(new uk.gov.moj.cpp.sjp.domain.Session(sessionId, userId, DELEGATED_POWERS, "lja"));

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.requestCaseAssignment(userId)).thenReturn(Stream.of(caseAssignmentRequested));

        assignmentHandler.assignCase(assignCaseCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(assignCaseCommand),
                                payloadIsJson(allOf(
                                        withJsonPath("$.session.id", equalTo(caseAssignmentRequested.getSession().getId().toString())),
                                        withJsonPath("$.session.userId", equalTo(caseAssignmentRequested.getSession().getUserId().toString())),
                                        withJsonPath("$.session.type", equalTo(caseAssignmentRequested.getSession().getType().toString())),
                                        withJsonPath("$.session.localJusticeAreaNationalCourtCode", equalTo(caseAssignmentRequested.getSession().getLocalJusticeAreaNationalCourtCode())))
                                )))));
    }


    public void shouldUnassignCase() throws EventStreamException {

        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope unassignCaseCommand = envelopeFrom(
                metadataWithRandomUUID(UNASSIGN_CASE_COMMAND),
                createObjectBuilder().add("caseId", caseId.toString()).build()
        );

        final CaseUnassigned caseUnassigned = new CaseUnassigned(caseId);

        when(eventSource.getStreamById(caseId)).thenReturn(case1EventStream);
        when(aggregateService.get(case1EventStream, CaseAggregate.class)).thenReturn(caseAggregate1);
        when(caseAggregate1.unassignCase()).thenReturn(Stream.of(caseUnassigned));

        assignmentHandler.unassignCase(unassignCaseCommand);

        assertThat(case1EventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(unassignCaseCommand),
                                payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString())))))));
    }

    @Test
    public void shouldHandleAssignCaseCommand() {
        assertThat(AssignmentHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(allOf(
                        method("assignCase").thatHandles(ASSIGN_CASE_COMMAND),
                        method("unassignCase").thatHandles(UNASSIGN_CASE_COMMAND),
                        method("assignCaseFromCandidatesList").thatHandles(ASSIGN_CASE__FROM_CANDIDATES_LIST_COMMAND)
                )));
    }
}
