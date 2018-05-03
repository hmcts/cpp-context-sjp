package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

@ServiceComponent(COMMAND_HANDLER)
public class CompleteCaseHandler extends CaseCommandHandler {

    @Handles("sjp.command.complete-case")
    public void completeCase(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, CaseAggregate::completeCase);
    }
}
