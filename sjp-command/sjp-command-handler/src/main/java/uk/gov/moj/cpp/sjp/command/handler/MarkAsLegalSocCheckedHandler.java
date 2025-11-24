package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class MarkAsLegalSocCheckedHandler extends CaseCommandHandler {

    @Handles("sjp.command.mark-as-legal-soc-checked")
    public void markAsLegalSocChecked(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID caseId = getCaseId(payload);
        final UUID userId = getUserId(command);
        final ZonedDateTime now = ZonedDateTime.now();

        applyToCaseAggregate(command, aCase -> aCase.markAsLegalSocChecked(caseId, userId, now));
    }
}
