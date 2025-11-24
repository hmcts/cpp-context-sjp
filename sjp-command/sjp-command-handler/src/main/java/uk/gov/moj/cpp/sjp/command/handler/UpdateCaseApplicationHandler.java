package uk.gov.moj.cpp.sjp.command.handler;


import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.UUID;

import static java.util.UUID.fromString;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCaseApplicationHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-cc-case-application-status")
    public void updateCaseApplication(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();
        final UUID applicationId = fromString(payload.getString("applicationId"));
        final UUID caseId = fromString(payload.getString("caseId"));
        final ApplicationStatus applicationStatus = ApplicationStatus.valueOf(payload.getString("applicationStatus"));

        applyToCaseAggregate(command, caseAggregate -> caseAggregate.updateCaseApplicationStatus(caseId, applicationId, applicationStatus));


    }
}
