package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class EmployerHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-employer")
    public void updateEmployer(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        applyToCaseAggregate(command, aggregate -> aggregate.updateEmployer(prepareEmployer(payload)));
    }

    private Employer prepareEmployer(JsonObject employerPayload) {
        final String defendantId = employerPayload.getString("defendantId");
        final String name = JsonObjects.getString(employerPayload, "name").orElse(null);
        final String employeeReference = JsonObjects.getString(employerPayload, "employeeReference").orElse(null);
        final String phone = JsonObjects.getString(employerPayload, "phone").orElse(null);
        final String address1 = JsonObjects.getString(employerPayload, "address", "address1").orElse(null);
        final String address2 = JsonObjects.getString(employerPayload, "address", "address2").orElse(null);
        final String address3 = JsonObjects.getString(employerPayload, "address", "address3").orElse(null);
        final String address4 = JsonObjects.getString(employerPayload, "address", "address4").orElse(null);
        final String postcode = JsonObjects.getString(employerPayload, "address", "postcode").orElse(null);

        return new Employer(UUID.fromString(defendantId), name, employeeReference, phone, new Address(address1, address2, address3, address4, postcode));
    }

    @Handles("sjp.command.delete-employer")
    public void deleteEmployer(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID defendantId = UUID.fromString(payload.getString("defendantId"));
        applyToCaseAggregate(command, aggregate -> aggregate.deleteEmployer(defendantId));
    }
}
