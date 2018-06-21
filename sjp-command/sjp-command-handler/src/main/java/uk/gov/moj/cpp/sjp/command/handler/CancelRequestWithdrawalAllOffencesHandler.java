package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class CancelRequestWithdrawalAllOffencesHandler extends CaseCommandHandler {

    @Handles("sjp.command.cancel-request-withdrawal-all-offences")
    public void cancelRequestWithdrawalAllOffences(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, CaseAggregate::cancelRequestWithdrawalAllOffences);
    }

}
