package uk.gov.moj.cpp.sjp.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.ASSIGNMENT_ASSIGNEE;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.ASSIGNMENT_DOMAIN_OBJECT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.ASSIGNMENT_NATURE_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_ASSIGNMENT_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.processor.service.AssignmentService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class AssignmentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentProcessor.class.getCanonicalName());

    static final String ASSIGNMENT_CONTEXT_ASSIGNMENT_CREATED = "assignment.assignment-created";
    static final String ASSIGNMENT_CONTEXT_ASSIGNMENT_DELETED = "assignment.assignment-deleted";

    static final String SJP_COMMAND_HANDLER_ASSIGNMENT_CREATED = "sjp.command.case-assignment-created";
    static final String SJP_COMMAND_HANDLER_ASSIGNMENT_DELETED = "sjp.command.case-assignment-deleted";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AssignmentService assignmentService;

    @Handles(ASSIGNMENT_CONTEXT_ASSIGNMENT_CREATED)
    public void handleAssignmentCreated(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String id = payload.getString(ASSIGNMENT_DOMAIN_OBJECT_ID, "NULL");
        LOGGER.debug("Received assignment created message for id: {}", id);

        final String caseAssignmentTypeString = payload.getString(ASSIGNMENT_NATURE_TYPE, "NULL");
        final Optional<CaseAssignmentType> caseAssignmentType = CaseAssignmentType.from(caseAssignmentTypeString);

        if (caseAssignmentType.isPresent()) {
            final Optional<UUID> assignee = Optional.ofNullable(payload.getString(ASSIGNMENT_ASSIGNEE, null)).map(UUID::fromString);
            sender.send(createEvent(SJP_COMMAND_HANDLER_ASSIGNMENT_CREATED, envelope, id, caseAssignmentType.get(), assignee));
        } else {
            LOGGER.debug("Ignoring non-ATCM assignment creation type: {}", caseAssignmentTypeString);
        }
    }

    @Handles(ASSIGNMENT_CONTEXT_ASSIGNMENT_DELETED)
    public void handleAssignmentDeleted(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String id = payload.getString(ASSIGNMENT_DOMAIN_OBJECT_ID, "NULL");
        LOGGER.debug("Received assignment deleted message for id: {}", id);

        final String caseAssignmentTypeString = payload.getString(ASSIGNMENT_NATURE_TYPE, "unknown");
        final Optional<CaseAssignmentType> caseAssignmentType = CaseAssignmentType.from(caseAssignmentTypeString);

        if (caseAssignmentType.isPresent()) {
            sender.send(createEvent(SJP_COMMAND_HANDLER_ASSIGNMENT_DELETED, envelope, id, caseAssignmentType.get(), Optional.empty()));
        } else {
            LOGGER.debug("Ignoring non-ATCM assignment deletion type: {}", caseAssignmentTypeString);
        }
    }

    @Handles("sjp.events.case-assignment-requested")
    public void handleCaseAssignmentRequest(final JsonEnvelope caseAssignmentRequest) {
        final JsonObject session = caseAssignmentRequest.payloadAsJsonObject().getJsonObject("session");

        final UUID sessionId = UUID.fromString(session.getString("id"));
        final SessionType sessionType = SessionType.valueOf(session.getString("type"));
        final UUID userId = UUID.fromString(session.getString("userId"));
        final String localJusticeAreaNationalCourtCode = session.getString("localJusticeAreaNationalCourtCode");

        final List<AssignmentCandidate> assignmentCandidates = assignmentService.getAssignmentCandidates(caseAssignmentRequest, userId, localJusticeAreaNationalCourtCode, sessionType);

        if (assignmentCandidates.isEmpty()) {
            notifyCaseNotFound(caseAssignmentRequest);
        } else {
            assignCase(caseAssignmentRequest, sessionId, assignmentCandidates);
        }
    }

    private void notifyCaseNotFound(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "public.sjp.case-not-assigned").apply(Json.createObjectBuilder().build()));
    }

    private void assignCase(final JsonEnvelope envelope, final UUID sessionId, final List<AssignmentCandidate> assignmentCandidates) {
        final JsonArrayBuilder assignmentCandidatesBuilder = Json.createArrayBuilder();
        assignmentCandidates.forEach(assignmentCandidate -> assignmentCandidatesBuilder.add(Json.createObjectBuilder()
                .add("caseId", assignmentCandidate.getCaseId().toString())
                .add("caseStreamVersion", assignmentCandidate.getCaseStreamVersion())));

        final JsonObject payload = Json.createObjectBuilder()
                .add("sessionId", sessionId.toString())
                .add("assignmentCandidates", assignmentCandidatesBuilder)
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.assign-case").apply(payload));
    }

    private JsonEnvelope createEvent(final String command,
                                     final JsonEnvelope envelope,
                                     final String id,
                                     final CaseAssignmentType caseAssignmentType,
                                     final Optional<UUID> assignee) {
        final JsonObjectBuilder publicEventPayloadBuilder = Json.createObjectBuilder()
                .add(CASE_ID, id)
                .add(CASE_ASSIGNMENT_TYPE, caseAssignmentType.toString());

        assignee.ifPresent(a -> publicEventPayloadBuilder.add("assigneeId", a.toString()));

        return enveloper.withMetadataFrom(envelope, command).apply(publicEventPayloadBuilder.build());
    }
}
