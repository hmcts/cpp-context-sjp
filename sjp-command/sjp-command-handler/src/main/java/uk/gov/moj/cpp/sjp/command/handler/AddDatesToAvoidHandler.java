package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddDatesToAvoidHandler extends CaseCommandHandler {

    @Handles("sjp.command.add-dates-to-avoid")
    public void addDatesToAvoid(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final String datesToAvoid = payload.getString("datesToAvoid");
        applyToCaseAggregate(command, aggregate -> aggregate.addDatesToAvoid(datesToAvoid));
    }
}
