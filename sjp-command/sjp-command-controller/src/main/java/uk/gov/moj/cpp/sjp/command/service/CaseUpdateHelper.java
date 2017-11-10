package uk.gov.moj.cpp.sjp.command.service;


import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class CaseUpdateHelper {

    private static final String CASE_ID = "caseId";
    @Inject
    private AssignmentQueryService assignmentService;
    @Inject
    private ResultingQueryService resultingService;
    @Inject
    private Enveloper enveloper;

    //should be subset of CaseUpdateRejected class
    public enum RejectReason {
        CASE_ASSIGNED,
        CASE_COMPLETED
    }

    public Optional<JsonEnvelope> checkForCaseUpdateRejectReasons(final JsonEnvelope envelope) {

        JsonEnvelope command = null;

        //Check is the requested case has been resulted or assigned for decision before being updated,
        //If resulted or assigned then update is rejected and delegated to the handler
        //Else continue with update of all offences against given case.

        final JsonEnvelope caseDecision = resultingService.findCaseDecision(envelope);
        final JsonArray caseDecisions = caseDecision.payloadAsJsonObject().getJsonArray("caseDecisions");
        if (caseDecisions.isEmpty()) {
            final JsonEnvelope assignmentDetails = assignmentService.findAssignmentDetails(envelope);
            final List<JsonObject> assignments = assignmentDetails
                    .payloadAsJsonObject()
                    .getJsonArray("assignments")
                    .getValuesAs(JsonObject.class);

            final String caller = envelope.metadata().userId().get();

            if (!assignments.isEmpty() &&
                    assignments.stream().noneMatch(assignment -> caller.equals((assignment).getString("assignee")))) {
                command = createCaseUpdateRejectedCommand(envelope, RejectReason.CASE_ASSIGNED.name());
            }
        } else {
            command = createCaseUpdateRejectedCommand(envelope, RejectReason.CASE_COMPLETED.name());
        }
        return Optional.ofNullable(command);
    }

    private JsonEnvelope createCaseUpdateRejectedCommand(final JsonEnvelope envelope, final String reason) {
        final String caseId = envelope.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = Json.createObjectBuilder()
                .add(CASE_ID, caseId)
                .add("reason", reason)
                .build();
        return enveloper
                .withMetadataFrom(envelope, "sjp.command.case-update-rejected")
                .apply(payload);
    }
}
