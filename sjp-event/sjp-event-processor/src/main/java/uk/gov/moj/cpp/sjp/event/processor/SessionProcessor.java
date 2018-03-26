package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.processor.service.AssignmentService;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class SessionProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AssignmentService assignmentService;


    @Handles("sjp.events.magistrate-session-started")
    public void magistrateSessionStarted(final JsonEnvelope sessionStartedEvent) {
        handleSessionStarted(sessionStartedEvent, MAGISTRATE);
    }

    @Handles("sjp.events.delegated-powers-session-started")
    public void delegatedPowersSessionStarted(final JsonEnvelope sessionStartedEvent) {
        handleSessionStarted(sessionStartedEvent, DELEGATED_POWERS);
    }

    private void handleSessionStarted(final JsonEnvelope sessionStartedEvent, final SessionType sessionType) {
        final JsonObject sessionDetails = sessionStartedEvent.payloadAsJsonObject();

        final UUID sessionId = UUID.fromString(sessionDetails.getString("sessionId"));
        final UUID legalAdviserId = UUID.fromString(sessionDetails.getString("legalAdviserId"));
        final String courtCode = sessionDetails.getString("courtCode");

        final List<AssignmentCandidate> assignmentCandidates = assignmentService.getAssignmentCandidates(sessionStartedEvent, legalAdviserId, courtCode, sessionType);

        if (assignmentCandidates.isEmpty()) {
            notifyCaseNotFound(sessionStartedEvent, sessionId);
        } else {
            assignCase(sessionStartedEvent, sessionId, assignmentCandidates);
        }
    }

    private void notifyCaseNotFound(final JsonEnvelope envelope, final UUID sessionId) {
        final JsonObject payload = Json.createObjectBuilder().add("sessionId", sessionId.toString()).build();
        sender.send(enveloper.withMetadataFrom(envelope, "public.sjp.session-started").apply(payload));
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

}
