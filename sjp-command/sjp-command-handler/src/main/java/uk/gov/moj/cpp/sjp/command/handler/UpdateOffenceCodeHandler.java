package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(COMMAND_HANDLER)
public class UpdateOffenceCodeHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-offence-code")
    public void updateOffenceCode(final JsonEnvelope envelope) throws EventStreamException {
        JsonObject payload = envelope.payloadAsJsonObject();

        final UUID caseId = getCaseId(payload);

        final String offenceCode = payload.getString("offenceCode");

        applyToCaseAggregate(envelope,
                aCase -> aCase.updateOffenceCode(caseId, offenceCode)
        );
    }
}

