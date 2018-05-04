package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;

import java.util.UUID;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class CaseUpdateRejectedHandler extends CaseCommandHandler {

    @Handles("sjp.command.case-update-rejected")
    public void caseUpdateRejected(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, aggregate -> aggregate.caseUpdateRejected(UUID.fromString(command.payloadAsJsonObject().getString(CASE_ID)),
                CaseUpdateRejected.RejectReason.valueOf(command.payloadAsJsonObject().getString("reason"))));
    }
}
