package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.UpdateEmployer;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;

import java.util.UUID;

@ServiceComponent(Component.COMMAND_HANDLER)
public class EmployerHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-employer")
    public void updateEmployer(final Envelope<UpdateEmployer> command) throws EventStreamException {
        applyToCaseAggregate(command.payload().getCaseId(), command, aggregate -> aggregate.updateEmployer(
                getUserId(command), prepareEmployer(command.payload())));
    }

    private Employer prepareEmployer(final UpdateEmployer employerPayload) {
        final uk.gov.moj.cpp.sjp.Employer employer = employerPayload.getEmployer();

        final UUID defendantId = employerPayload.getDefendantId();
        final String name = employer.getName().orElse(null);
        final String employeeReference = employer.getEmployeeReference().orElse(null);
        final String phone = employer.getPhone().orElse(null);

        final Address address =
                employer.getAddress()
                        .map(val ->
                                new Address(
                                        val.getAddress1().orElse(null),
                                        val.getAddress2().orElse(null),
                                        val.getAddress3().orElse(null),
                                        val.getAddress4().orElse(null),
                                        // val.getAddress5().orElse(null), TODO: add back this when framework fixed! Currently val.getAddress5() is null.
                                        val.getPostcode().orElse(null)
                                )
                        )
                        .orElse(Address.UNKNOWN);

        return new Employer(defendantId, name, employeeReference, phone, address);
    }

    @Handles("sjp.command.delete-employer")
    public void deleteEmployer(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, aggregate -> aggregate.deleteEmployer(
                getUserId(command),
                UUID.fromString(command.payloadAsJsonObject().getString("defendantId"))));
    }
}
