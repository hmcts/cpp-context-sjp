package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ProvedInAbsenceHandler extends CaseCommandHandler {

    @Handles("sjp.command.expire-defendant-response-timer")
    public void expireDefendantResponseTimer(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, CaseAggregate::expireDefendantResponseTimer);
    }
}
