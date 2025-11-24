package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.DELEGATED_POWERS_DECISION;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.STALE_CANDIDATES;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AssignmentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentHandler.class);

    @Inject
    private Clock clock;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.command.assign-next-case")
    public void assignNextCase(final JsonEnvelope command) throws EventStreamException {
        final UUID sessionId = UUID.fromString(command.payloadAsJsonObject().getString("sessionId"));
        final UUID userId = UUID.fromString(command.metadata().userId().get());

        final EventStream sessionEventStream = eventSource.getStreamById(sessionId);
        final Session session = aggregateService.get(sessionEventStream, Session.class);
        final Stream<Object> events = session.requestCaseAssignment(userId);

        sessionEventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    @Handles("sjp.command.assign-case")
    public void assignCase(final JsonEnvelope command) throws EventStreamException {
        final UUID caseToBeAssignedId = fromString(command.payloadAsJsonObject().getString("assignCase"));
        final UUID userId = fromString(command.payloadAsJsonObject().getString("userId"));

        for (final JsonString unassignCaseId : command.payloadAsJsonObject().getJsonArray("unassignCases").getValuesAs(JsonString.class)) {
            final UUID caseId = fromString(unassignCaseId.getString());
            final EventStream caseEventStream = eventSource.getStreamById(caseId);
            final CaseAggregate caseAggregate = aggregateService.get(caseEventStream, CaseAggregate.class);

            final Stream<Object> events = caseAggregate.unassignCase();

            caseEventStream.append(events.map(enveloper.withMetadataFrom(command)));
        }

        final EventStream caseEventStream = eventSource.getStreamById(caseToBeAssignedId);
        final CaseAggregate aCase = aggregateService.get(caseEventStream, CaseAggregate.class);

        final Stream<Object> events = aCase.assignCaseToUser(userId, clock.now());

        caseEventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    @Handles("sjp.command.unassign-case")
    public void unassignCase(final JsonEnvelope command) throws EventStreamException {
        final UUID caseId = fromString(command.payloadAsJsonObject().getString("caseId"));

        final EventStream caseEventStream = eventSource.getStreamById(caseId);
        final CaseAggregate aCase = aggregateService.get(caseEventStream, CaseAggregate.class);
        final Stream<Object> events = aCase.unassignCase();

        caseEventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    @Handles("sjp.command.assign-case-from-candidates-list")
    public void assignCaseFromCandidatesList(final JsonEnvelope command) throws EventStreamException {
        final UUID sessionId = fromString(command.payloadAsJsonObject().getString("sessionId"));

        final EventStream sessionEventStream = eventSource.getStreamById(sessionId);
        final Session session = aggregateService.get(sessionEventStream, Session.class);

        final List<AssignmentCandidate> assignmentCandidates = command.payloadAsJsonObject()
                .getJsonArray("assignmentCandidates")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(assignmentCandidate -> new AssignmentCandidate(fromString(assignmentCandidate.getString("caseId")), assignmentCandidate.getInt("caseStreamVersion")))
                .collect(toList());

        for (final AssignmentCandidate assignmentCandidate : assignmentCandidates) {
            final EventStream caseEventsStream = eventSource.getStreamById(assignmentCandidate.getCaseId());
            long actualCaseStreamVersion = caseEventsStream.size();

            if (actualCaseStreamVersion == assignmentCandidate.getCaseStreamVersion()) {
                final CaseAggregate caseAggregate = aggregateService.get(caseEventsStream, CaseAggregate.class);

                final CaseAssignmentType caseAssignmentType = session.getSessionType().equals(SessionType.MAGISTRATE) ? MAGISTRATE_DECISION : DELEGATED_POWERS_DECISION;

                final Stream<Object> assignmentEvents = caseAggregate.assignCase(session.getUser(), clock.now(), caseAssignmentType);

                caseEventsStream.appendAfter(assignmentEvents.map(enveloper.withMetadataFrom(command)), assignmentCandidate.getCaseStreamVersion());

                return;
            } else {
                LOGGER.warn("Assignment candidate {} qualified based on stale data. Used case version: {}, actual version: {}", assignmentCandidate.getCaseId(), assignmentCandidate.getCaseStreamVersion(), actualCaseStreamVersion);
            }
        }

        sessionEventStream.append(session.rejectCaseAssignment(STALE_CANDIDATES).map(enveloper.withMetadataFrom(command)));
    }
}
