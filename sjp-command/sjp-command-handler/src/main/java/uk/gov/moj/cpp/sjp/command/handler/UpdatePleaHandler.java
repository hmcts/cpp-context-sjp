package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdatePleaHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-plea")
    public void updatePlea(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, aCase ->
                aCase.updatePlea(converter.convert(command.payloadAsJsonObject(), UpdatePlea.class)));
    }

    @Handles("sjp.command.cancel-plea")
    public void cancelPlea(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, aCase ->
                aCase.cancelPlea(converter.convert(command.payloadAsJsonObject(), CancelPlea.class)));
    }
}
