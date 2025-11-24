package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ChangeCaseManagementStatusHandler extends CaseCommandHandler {
    private static final String COMMAND_NAME = "sjp.command.change-case-management-status";

    @Handles(COMMAND_NAME)
    public void changeCaseManagementStatus(final JsonEnvelope updateCaseManagementStatusCommand) throws EventStreamException {
        final JsonObject payload = updateCaseManagementStatusCommand.payloadAsJsonObject();
        final CaseManagementStatus caseManagementStatus = CaseManagementStatus.valueOf(payload.getString("caseManagementStatus"));

        applyToCaseAggregate(updateCaseManagementStatusCommand, caseAggregate -> caseAggregate.changeCaseManagementStatus(caseManagementStatus));
    }
}
