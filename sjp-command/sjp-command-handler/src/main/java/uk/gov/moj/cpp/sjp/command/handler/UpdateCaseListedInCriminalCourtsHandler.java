package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCaseListedInCriminalCourtsHandler extends CaseCommandHandler {

    public static final String COMMAND_NAME = "sjp.command.update-case-listed-in-criminal-courts";

    @Handles(UpdateCaseListedInCriminalCourtsHandler.COMMAND_NAME)
    public void updateCaseListedInCriminalCourts(final JsonEnvelope updateCaseListedInCriminalCourtsCommand) throws EventStreamException {

        final JsonObject payload = updateCaseListedInCriminalCourtsCommand.payloadAsJsonObject();

        final UUID caseId = getCaseId(payload);

        applyToCaseAggregate(updateCaseListedInCriminalCourtsCommand, aCase -> aCase.updateCaseListedInCriminalCourts(caseId));
    }

}
