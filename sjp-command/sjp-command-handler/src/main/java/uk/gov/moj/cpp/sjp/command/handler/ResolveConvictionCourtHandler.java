package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;

import java.util.Map;
import java.util.UUID;

@ServiceComponent(COMMAND_HANDLER)
public class ResolveConvictionCourtHandler extends CaseCommandHandler {

    @Handles("sjp.command.resolve-conviction-court-bdf")
    public void resolveConvictionCourt(final JsonEnvelope command) throws EventStreamException {
        final UUID caseId = UUID.fromString(command.payloadAsJsonObject().getString("caseId"));
        final Map<UUID, Session> sessions = getSessionFromCaseAggregate(caseId);
        applyToCaseAggregate(caseId, command, caseAggregate -> caseAggregate.resolveConvictionCourt(caseId, sessions));
    }
}
