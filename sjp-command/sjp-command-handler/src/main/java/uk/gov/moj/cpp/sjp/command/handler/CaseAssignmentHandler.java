package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseAssignment;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CaseAssignmentHandler extends CaseCommandHandler {

    private static final String SJP_COMMAND_HANDLER_ASSIGNMENT_CREATED = "sjp.command.case-assignment-created";
    private static final String SJP_COMMAND_HANDLER_ASSIGNMENT_DELETED = "sjp.command.case-assignment-deleted";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles(SJP_COMMAND_HANDLER_ASSIGNMENT_CREATED)
    public void assignmentCreated(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        final CaseAssignment caseAssignment = jsonObjectToObjectConverter.convert(payload, CaseAssignment.class);

        applyToCaseAggregate(command, aCase -> aCase.caseAssignmentCreated(caseAssignment));
    }

    @Handles(SJP_COMMAND_HANDLER_ASSIGNMENT_DELETED)
    public void assignmentDeleted(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        final CaseAssignment caseAssignment = jsonObjectToObjectConverter.convert(payload, CaseAssignment.class);

        applyToCaseAggregate(command, aCase -> aCase.caseAssignmentDeleted(caseAssignment));
    }
}
