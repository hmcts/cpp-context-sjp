package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefendantNationalInsuranceNumberHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-defendant-national-insurance-number")
    public void updateDefendantNationalInsuranceNumber(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID defendantId = UUID.fromString(payload.getString("defendantId"));
        final String nationalInsuranceNumber = payload.getString("nationalInsuranceNumber", null);
        applyToCaseAggregate(command, aggregate -> aggregate.updateDefendantNationalInsuranceNumber(
                getUserId(command), defendantId, nationalInsuranceNumber));
    }
}
