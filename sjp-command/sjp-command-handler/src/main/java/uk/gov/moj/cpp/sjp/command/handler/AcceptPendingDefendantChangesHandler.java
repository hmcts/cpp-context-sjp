package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.Objects.isNull;

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

@SuppressWarnings("squid:CallToDeprecatedMethod")
@ServiceComponent(Component.COMMAND_HANDLER)
public class AcceptPendingDefendantChangesHandler extends BasePersonInfoHandler {
    @Inject
    private EventSource eventSource;
    @Inject
    private Enveloper enveloper;
    @Inject
    private AggregateService aggregateService;

    @Handles("sjp.command.accept-pending-defendant-changes")
    public void acceptPendingDefendantChanges(final JsonEnvelope command) throws EventStreamException {

        final ZonedDateTime createdAt = command.metadata().createdAt().orElseGet(ZonedDateTime::now);
        final JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString("caseId"));
        final UUID defendantId = UUID.fromString(payload.getString("defendantId"));

        final String firstName = getStringOrNull(payload, "firstName");
        final String lastName = getStringOrNull(payload, "lastName");
        final String dateOfBirth = getStringOrNull(payload, "dateOfBirth");
        final String legalEntityName = getStringOrNull(payload, "legalEntityName");

        final Address address = isNull(payload.getJsonObject("address")) ? null : createAddressFrom(payload);
        final LocalDate birthDate = dateOfBirth == null ? null : LocalDate.parse(dateOfBirth);

        final EventStream eventStream = eventSource.getStreamById(caseId);

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Person person = new Person(null, firstName, lastName, birthDate, null, null, null, null, address, null, null, legalEntityName);

        final Stream<Object> events = caseAggregate.acceptPendingDefendantChanges(getUserId(command), caseId, defendantId, person, createdAt);

        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }
}
