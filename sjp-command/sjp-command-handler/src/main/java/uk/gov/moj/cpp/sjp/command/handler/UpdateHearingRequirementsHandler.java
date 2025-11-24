package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateHearingRequirementsHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-hearing-requirements")
    public void updateHearingRequirements(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID defendantId = UUID.fromString(payload.getString("defendantId"));
        final String interpreterLanguage = payload.getString("interpreterLanguage", null);
        final Boolean speakWelsh = JsonObjects.getBoolean(payload, "speakWelsh").orElse(null);

        applyToCaseAggregate(command, aggregate -> aggregate.updateHearingRequirements(
                getUserId(command), defendantId, interpreterLanguage, speakWelsh));
    }
}
