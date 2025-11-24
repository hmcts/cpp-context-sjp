package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadAocpOnline;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class DefendantAcceptedAOCPPleadOnlineHandler extends CaseCommandHandler {

    @Inject
    private Clock clock;

    @Handles("sjp.command.plead-aocp-online")
    public void pleadAocpAcceptedOnline(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final PleadAocpOnline pleadAocpOnline = converter.convert(payload, PleadAocpOnline.class);

        applyToCaseAggregate(command, aCase -> aCase.pleadAocpAcceptedOnline(pleadAocpOnline, clock.now()));
    }
}
