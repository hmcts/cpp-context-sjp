package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CaseAssignmentHandler extends CaseCommandHandler {

    @Handles("sjp.command.case-assignment-created")
    public void assignmentCreated(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString("caseId"));
        final UUID assigneeId = UUID.fromString(payload.getString("assigneeId"));
        final CaseAssignmentType caseAssignmentType = CaseAssignmentType.from(payload.getString("caseAssignmentType")).get();

        applyToCaseAggregate(command, aCase -> aCase.caseAssignmentCreated(caseId, assigneeId, caseAssignmentType));
    }

    @Handles("sjp.command.case-assignment-deleted")
    public void assignmentDeleted(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString("caseId"));
        final CaseAssignmentType caseAssignmentType = CaseAssignmentType.from(payload.getString("caseAssignmentType")).get();

        applyToCaseAggregate(command, aCase -> aCase.caseAssignmentDeleted(caseId, caseAssignmentType));
    }
}
