package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.PersonInfoDetails;
import uk.gov.moj.cpp.sjp.domain.aggregate.DefendantAggregate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdatePersonInfoHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AggregateService aggregateService;

    @Handles("sjp.command.update-person-info")
    public void updatePersonInfo(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();

        final Address address = new Address(payload.getString("address1", null),
                payload.getString("address2", null),
                payload.getString("address3", null),
                payload.getString("address4", null),
                payload.getString("postCode", null));


        final PersonInfoDetails personInfoDetails = new PersonInfoDetails(
                UUID.fromString(payload.getString("personId")),
                payload.getString("title", null),
                payload.getString("firstName", null),
                payload.getString("lastName", null),
                Optional.ofNullable(payload.getString("dateOfBirth", null)).map(LocalDate::parse).orElse(null),
                address
        );

        final EventStream streamById = eventSource.getStreamById(personInfoDetails.getPersonId());
        final DefendantAggregate defendantAggregate = aggregateService.get(streamById, DefendantAggregate.class);

        streamById
            .append(defendantAggregate.updatePersonInfo(personInfoDetails).map(enveloper.withMetadataFrom(command)));
    }

}
