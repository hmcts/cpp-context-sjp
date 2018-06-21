package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateInterpreterHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-interpreter")
    public void updateInterpreter(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID defendantId = UUID.fromString(payload.getString("defendantId"));
        final String interpreterLanguage = payload.getString("language", null);
        applyToCaseAggregate(command, aggregate -> aggregate.updateInterpreter(
                getUserId(command), defendantId, interpreterLanguage));
    }
}
