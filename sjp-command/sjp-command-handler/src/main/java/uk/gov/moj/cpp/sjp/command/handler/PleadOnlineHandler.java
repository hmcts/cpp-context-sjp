package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnlinePcqVisited;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PleadOnlineHandler extends CaseCommandHandler {

    @Inject
    private Clock clock;

    @Handles("sjp.command.plead-online")
    public void pleadOnline(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID caseId = getCaseId(payload);
        final PleadOnline pleadOnline = converter.convert(payload, PleadOnline.class);
        final UUID userId = getUserId(command);

        applyToCaseAggregate(command, aCase -> aCase.pleadOnline(caseId, pleadOnline, clock.now(), userId));
    }

    @Handles("sjp.command.plead-online-pcq-visited")
    public void pleadOnlinePcqVisited(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID caseId = getCaseId(payload);
        final PleadOnlinePcqVisited pleadOnlinePcqVisited = converter.convert(payload, PleadOnlinePcqVisited.class);

        applyToCaseAggregate(command, aCase -> aCase.pleadOnlinePcqVisited(caseId, pleadOnlinePcqVisited, clock.now()));
    }
}
