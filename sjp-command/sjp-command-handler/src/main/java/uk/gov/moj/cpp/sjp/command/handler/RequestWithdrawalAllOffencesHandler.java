package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class RequestWithdrawalAllOffencesHandler extends CaseCommandHandler {

    @Handles("sjp.command.request-withdrawal-all-offences")
    public void requestWithdrawalAllOffences(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, aggregate -> aggregate.requestWithdrawalAllOffences(command.payloadAsJsonObject().getString(CASE_ID)));
    }
}
