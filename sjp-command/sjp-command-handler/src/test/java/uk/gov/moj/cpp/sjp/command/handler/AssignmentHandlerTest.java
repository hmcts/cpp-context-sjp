package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
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
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.event.CaseAssigned;

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
    private static final String CASE_ASSIGNED_EVENT = "sjp.events.case-assigned";

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
    private Enveloper enveloper = createEnveloperWithEvents(CaseAssigned.class);

    @InjectMocks
    private AssignmentHandler assignmentHandler;

    @Test
    public void shouldAssignFirstAssignmentCandidateThatHasTheSameVersionAsCorrespondingCaseEventStream() throws EventStreamException {

        final UUID sessionId = randomUUID();
        final UUID assigneeId = randomUUID();

        final UUID assignmentCandidate1Id = randomUUID();
        final UUID assignmentCandidate2Id = randomUUID();
        final UUID assignmentCandidate3Id = randomUUID();

        long assignmentCandidate1Version = 10;
        long assignmentCandidate2Version = 10;
        long assignmentCandidate3Version = 10;

        final JsonEnvelope assignCaseCommand = envelopeFrom(metadataWithRandomUUID(ASSIGN_CASE_COMMAND), createObjectBuilder()
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

        final CaseAssigned caseAssigned = new CaseAssigned(assignmentCandidate2Id, sessionId, assigneeId, MAGISTRATE_DECISION);

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
        when(caseAggregate2.assignCase(sessionId, assigneeId, MAGISTRATE_DECISION)).thenReturn(Stream.of(caseAssigned));

        assignmentHandler.assignCase(assignCaseCommand);

        verify(case1EventStream, never()).append(any());
        verify(case3EventStream, never()).append(any());
        verify(eventSource, never()).getStreamById(assignmentCandidate3Id);

        assertThat(case2EventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(assignCaseCommand)
                                        .withName(CASE_ASSIGNED_EVENT),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(assignmentCandidate2Id.toString())),
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.assigneeId", equalTo(assigneeId.toString())),
                                        withJsonPath("$.caseAssignmentType", equalTo(MAGISTRATE_DECISION.toString()))
                                )))
                                .thatMatchesSchema())));
    }

    @Test
    public void shouldHandleAssignCaseCommand() {
        assertThat(AssignmentHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("assignCase").thatHandles(ASSIGN_CASE_COMMAND)));
    }
}
