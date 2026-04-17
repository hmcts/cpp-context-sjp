package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesAccepted;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AcceptPendingDefendantChangesHandlerCCTest extends CaseAggregateBaseTest {
    private static final String FIRST_NAME = "test";
    @Spy
    private Clock clock = new UtcClock();
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantNameUpdated.class, DefendantDateOfBirthUpdated.class, DefendantAddressUpdated.class,
            DefendantDetailsUpdated.class, DefendantPendingChangesAccepted.class);
    @InjectMocks
    private AcceptPendingDefendantChangesHandlerCC acceptPendingDefendantChangesHandlerCC;
    @Mock
    private EventStream eventStream;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;

    private static JsonObject toJsonObject(final Address address) {
        return createObjectBuilder()
                .add("address1", address.getAddress1())
                .add("address2", address.getAddress2())
                .add("address3", address.getAddress3())
                .add("address4", address.getAddress4())
                .add("address5", address.getAddress5())
                .add("postcode", address.getPostcode())
                .build();
    }

    @Test
    public void shouldAcceptPendingDefendantChangesFromCC() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();

        final JsonEnvelope command = createAcceptPendingDefendantChangesFromCCCommand();

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        acceptPendingDefendantChangesHandlerCC.acceptPendingDefendantChangesFromCC(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-name-updated"),
                                payloadIsJson(withJsonPath("$.newPersonalName.firstName", equalTo(FIRST_NAME)))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.firstName", equalTo(FIRST_NAME)),
                                        withJsonPath("$.lastName", equalTo(defendant.getLastName())),
                                        withJsonPath("$.dateOfBirth", equalTo(defendant.getDateOfBirth().format(ofPattern("YYY-MM-dd"))))))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-pending-changes-accepted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())))))
                )));
    }

    @Test
    public void shouldAcceptPendingDefendantChangesFromCCWhenCaseIsCompleted() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();
        final JsonEnvelope command = createAcceptPendingDefendantChangesFromCCCommandWithUpdatedAddress();

        caseAggregate.getState().markCaseCompleted();

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        acceptPendingDefendantChangesHandlerCC.acceptPendingDefendantChangesFromCC(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-address-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.newAddress.address1", equalTo("Flat 2")),
                                        withJsonPath("$.newAddress.postcode", equalTo("RG2 8DS"))))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updated"),
                                payloadIsJson(withJsonPath("$.defendantId", equalTo(defendantId.toString())))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-pending-changes-accepted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())))))
                )));
    }

    @Test
    public void shouldAcceptPendingDefendantChangesFromCCWhenCaseIsReferredForCourtHearing() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();
        final JsonEnvelope command = createAcceptPendingDefendantChangesFromCCCommandWithUpdatedAddress();

        caseAggregate.getState().markCaseReferredForCourtHearing();

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        acceptPendingDefendantChangesHandlerCC.acceptPendingDefendantChangesFromCC(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-address-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.newAddress.address1", equalTo("Flat 2")),
                                        withJsonPath("$.newAddress.postcode", equalTo("RG2 8DS"))))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updated"),
                                payloadIsJson(withJsonPath("$.defendantId", equalTo(defendantId.toString())))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-pending-changes-accepted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())))))
                )));
    }

    private JsonEnvelope createAcceptPendingDefendantChangesFromCCCommand() {
        final Defendant defendant = caseReceivedEvent.getDefendant();

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendant.getId().toString())
                .add("caseId", caseReceivedEvent.getCaseId().toString())
                .add("firstName", FIRST_NAME)
                .add("lastName", defendant.getLastName())
                .add("dateOfBirth", defendant.getDateOfBirth().format(ofPattern("YYY-MM-dd")))
                .add("address", toJsonObject(defendant.getAddress()));

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.accept-pending-defendant-changes-from-CC"),
                payload.build());
    }

    private JsonEnvelope createAcceptPendingDefendantChangesFromCCCommandWithUpdatedAddress() {
        final Defendant defendant = caseReceivedEvent.getDefendant();
        final Address updatedAddress = new Address("Flat 2","1 Oxford Road","","","","RG2 8DS");
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendant.getId().toString())
                .add("caseId", caseReceivedEvent.getCaseId().toString())
                .add("firstName", defendant.getFirstName())
                .add("lastName", defendant.getLastName())
                .add("dateOfBirth", defendant.getDateOfBirth().format(ofPattern("YYY-MM-dd")))
                .add("address", toJsonObject(updatedAddress));

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.accept-pending-defendant-changes-from-CC"),
                payload.build());
    }

}

