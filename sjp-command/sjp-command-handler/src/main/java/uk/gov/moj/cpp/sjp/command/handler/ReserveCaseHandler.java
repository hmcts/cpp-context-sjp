package uk.gov.moj.cpp.sjp.command.handler;

import java.util.UUID;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ReserveCaseHandler extends CaseCommandHandler {

    private static final String COMMAND_NAME = "sjp.command.reserve-case";

    @Handles(COMMAND_NAME)
    public void reserveCase(final JsonEnvelope jsonEnvelope) throws EventStreamException {
        final UUID reservedBy = getUserId(jsonEnvelope);

        applyToCaseAggregate(jsonEnvelope, aggregate -> aggregate.reserveCase(reservedBy));
    }

    @Handles("sjp.command.undo-reserve-case")
    public void undoReserveCase(final JsonEnvelope jsonEnvelope) throws EventStreamException {
        final UUID reservedBy = getUserId(jsonEnvelope);

        applyToCaseAggregate(jsonEnvelope, aggregate -> aggregate.undoReserveCase(reservedBy));
    }
}