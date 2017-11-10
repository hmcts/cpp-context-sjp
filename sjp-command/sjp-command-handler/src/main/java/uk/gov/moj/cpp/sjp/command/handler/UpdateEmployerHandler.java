package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Employer;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateEmployerHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-employer")
    public void updateEmployer(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final Employer employer = converter.convert(payload, Employer.class);
        applyToCaseAggregate(command, aggregate -> aggregate.updateEmployer(employer));
    }
}
