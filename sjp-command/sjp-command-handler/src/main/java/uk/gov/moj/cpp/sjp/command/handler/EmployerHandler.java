package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Employer;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class EmployerHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-employer")
    public void updateEmployer(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final Employer employer = converter.convert(payload, Employer.class);
        applyToCaseAggregate(command, aggregate -> aggregate.updateEmployer(employer));
    }

    @Handles("sjp.command.delete-employer")
    public void deleteEmployer(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID defendantId = UUID.fromString(payload.getString("defendantId"));
        applyToCaseAggregate(command, aggregate -> aggregate.deleteEmployer(defendantId));
    }
}
