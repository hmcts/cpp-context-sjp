package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PleaOnlineHandler extends CaseCommandHandler {

    @Handles("sjp.command.plea-online")
    public void pleaOnline(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID caseId = getCaseId(payload);
        final PleadOnline pleadOnline = converter.convert(payload, PleadOnline.class);

        applyToCaseAggregate(command, aCase -> aCase.pleaOnline(caseId, pleadOnline));
    }
}
