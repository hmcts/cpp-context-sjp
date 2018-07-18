package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.common.util.Clock;
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
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefendantDetailsHandler extends BasePersonInfoHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Clock clock;

    @Handles("sjp.command.update-defendant-details")
    public void updateDefendantDetails(final JsonEnvelope command) throws EventStreamException {

        final ZonedDateTime createdAt = command.metadata().createdAt().orElseGet(clock::now);

        final JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString("caseId"));
        final UUID defendantId = UUID.fromString(payload.getString("defendantId"));

        final String title = getStringOrNull(payload, "title");
        final String firstName = getStringOrNull(payload, "firstName");
        final String lastName = getStringOrNull(payload, "lastName");
        final String gender = getStringOrNull(payload, "gender");
        final String nationalInsuranceNumber = getStringOrNull(payload, "nationalInsuranceNumber");
        final String dateOfBirth = getStringOrNull(payload, "dateOfBirth");
        final String email = getStringOrNull(payload, "email");

        final JsonObject contactNumberPayload = payload.getJsonObject("contactNumber");
        final String homeNumber = getStringOrNull(contactNumberPayload, "home");
        final String mobileNumber = getStringOrNull(contactNumberPayload, "mobile");
        final Address address = createAddressFrom(payload);
        final LocalDate birthDate = dateOfBirth == null ? null : LocalDate.parse(dateOfBirth);

        final EventStream eventStream = eventSource.getStreamById(caseId);

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Person personInfoDetails = new Person(title, firstName, lastName, birthDate, gender, address);
        final Stream<Object> events = caseAggregate.updateDefendantDetails(caseId, defendantId, gender,
                nationalInsuranceNumber, email, homeNumber, mobileNumber, personInfoDetails, createdAt);

        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }
}

