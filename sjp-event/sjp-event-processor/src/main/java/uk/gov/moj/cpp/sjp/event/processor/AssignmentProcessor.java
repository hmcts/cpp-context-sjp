package uk.gov.moj.cpp.sjp.event.processor;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.UNKNOWN;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.ASSIGNEE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ASSIGNMENT_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.REASON;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseAssignmentTimeoutProcess;
import uk.gov.moj.cpp.sjp.event.processor.service.AssignmentService;
import uk.gov.moj.cpp.sjp.event.session.CaseAlreadyAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRequested;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class AssignmentProcessor {

    public static final String PUBLIC_SJP_CASE_ASSIGNED = "public.sjp.case-assigned";
    public static final String PUBLIC_SJP_CASE_NOT_ASSIGNED = "public.sjp.case-not-assigned";
    public static final String PUBLIC_SJP_CASE_ASSIGNMENT_REJECTED = "public.sjp.case-assignment-rejected";

    private static final Duration CASE_TIMEOUT_DURATION = Duration.ofMinutes(60);

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AssignmentService assignmentService;

    @Inject
    private CaseAssignmentTimeoutProcess caseAssignmentTimeoutProcess;


    @Handles(CaseAssignmentRequested.EVENT_NAME)
    public void handleCaseAssignmentRequestedEvent(final JsonEnvelope caseAssignmentRequest) {
        final JsonObject session = caseAssignmentRequest.payloadAsJsonObject().getJsonObject("session");

        final UUID sessionId = UUID.fromString(session.getString("id"));
        final SessionType sessionType = SessionType.valueOf(session.getString("type"));
        final UUID userId = UUID.fromString(session.getString("userId"));
        final String localJusticeAreaNationalCourtCode = session.getString("localJusticeAreaNationalCourtCode");

        final List<AssignmentCandidate> assignmentCandidates = assignmentService.getAssignmentCandidates(caseAssignmentRequest, userId, localJusticeAreaNationalCourtCode, sessionType);

        if (assignmentCandidates.isEmpty()) {
            emitCaseNotAssignedPublicEvent(caseAssignmentRequest);
        } else {
            assignCase(caseAssignmentRequest, sessionId, assignmentCandidates);
        }
    }

    @Handles(CaseAssignmentRejected.EVENT_NAME)
    public void handleCaseAssignmentRejectedEvent(final JsonEnvelope caseAssignmentRejectedEvent) {
        final JsonObject publicEventPayload = createObjectBuilder()
                .add(REASON, caseAssignmentRejectedEvent.payloadAsJsonObject().getString(REASON))
                .build();

        sender.send(enveloper.withMetadataFrom(caseAssignmentRejectedEvent, PUBLIC_SJP_CASE_ASSIGNMENT_REJECTED)
                .apply(publicEventPayload));
    }

    @Handles(CaseAssigned.EVENT_NAME)
    public void handleCaseAssignedEvent(final JsonEnvelope caseAssignedEvent) {
        final JsonObject caseAssigned = caseAssignedEvent.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(caseAssigned.getString(CASE_ID));
        final String assigneeId = caseAssigned.getString(ASSIGNEE_ID);
        final CaseAssignmentType caseAssignmentType = CaseAssignmentType.from(caseAssigned.getString(CASE_ASSIGNMENT_TYPE)).orElse(UNKNOWN);

        final JsonObject assignmentReplicationPayload = createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("version", 0)
                .add("domainObjectId", caseId.toString())
                .add("assignmentNatureType", caseAssignmentType.toString())
                .add("assignee", assigneeId)
                .build();

        //TODO remove (ATCM-3097)
        sender.send(enveloper.withMetadataFrom(caseAssignedEvent, "assignment.command.add-assignment-to")
                .apply(assignmentReplicationPayload));

        caseAssignmentTimeoutProcess.startTimer(caseId, CASE_TIMEOUT_DURATION);

        emitCaseAssignedPublicEvent(caseId, caseAssignedEvent);
    }

    @Handles(CaseAlreadyAssigned.EVENT_NAME)
    public void handleCaseAlreadyAssignedEvent(final JsonEnvelope caseAssignedEvent) {
        final UUID caseId = UUID.fromString(caseAssignedEvent.payloadAsJsonObject().getString(CASE_ID));

        caseAssignmentTimeoutProcess.resetTimer(caseId, CASE_TIMEOUT_DURATION);

        emitCaseAssignedPublicEvent(caseId, caseAssignedEvent);
    }

    @Handles(CaseUnassigned.EVENT_NAME)
    public void handleCaseUnassignedEvent(final JsonEnvelope caseUnassignedEvent) {

        final UUID caseId = UUID.fromString(caseUnassignedEvent.payloadAsJsonObject().getString(CASE_ID));

        final JsonObject assignmentReplicationPayload = createObjectBuilder()
                .add("domainObjectId", caseId.toString())
                .build();

        caseAssignmentTimeoutProcess.cancelTimer(caseId);

        //TODO remove (ATCM-3097)
        sender.send(enveloper.withMetadataFrom(caseUnassignedEvent, "assignment.command.remove-assignment")
                .apply(assignmentReplicationPayload));
    }

    private void emitCaseNotAssignedPublicEvent(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_SJP_CASE_NOT_ASSIGNED)
                .apply(createObjectBuilder().build()));
    }

    private void emitCaseAssignedPublicEvent(final UUID caseId, final JsonEnvelope event) {
        final JsonObject publicEventPayload = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build();

        sender.send(enveloper.withMetadataFrom(event, PUBLIC_SJP_CASE_ASSIGNED)
                .apply(publicEventPayload));
    }

    private void assignCase(final JsonEnvelope envelope, final UUID sessionId, final List<AssignmentCandidate> assignmentCandidates) {
        final JsonArrayBuilder assignmentCandidatesBuilder = Json.createArrayBuilder();
        assignmentCandidates.forEach(assignmentCandidate -> assignmentCandidatesBuilder.add(createObjectBuilder()
                .add("caseId", assignmentCandidate.getCaseId().toString())
                .add("caseStreamVersion", assignmentCandidate.getCaseStreamVersion())));

        final JsonObject payload = createObjectBuilder()
                .add("sessionId", sessionId.toString())
                .add("assignmentCandidates", assignmentCandidatesBuilder)
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.assign-case-from-candidates-list").apply(payload));
    }
}
