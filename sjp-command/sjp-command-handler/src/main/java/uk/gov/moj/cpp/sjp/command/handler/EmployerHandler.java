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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

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
        final String name = employer.getName();
        final String employeeReference = employer.getEmployeeReference();
        final String phone = employer.getPhone();

        final Address address =
                Optional.ofNullable(employer.getAddress())
                        .filter(a -> Stream.of(a.getAddress1(), a.getAddress2(), a.getAddress3(), a.getAddress4(), a.getAddress5(), a.getPostcode()).anyMatch(Objects::nonNull))
                        .map(a -> new Address(
                                a.getAddress1(),
                                a.getAddress2(),
                                a.getAddress3(),
                                a.getAddress4(),
                                a.getAddress5(),
                                a.getPostcode()))
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
