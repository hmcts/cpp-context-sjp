package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
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
import uk.gov.moj.cpp.sjp.event.*;

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
public class UpdateDefendantDetailsFromCCHandlerTest extends CaseAggregateBaseTest {

    private static final String FIRST_NAME = "test";
    private static final String EMAIL = "email";
    private static final String REGION = "REGION";
    private static final String DRIVER_NUMBER = "MORGA753116SM9IJ";
    @Spy
    private Clock clock = new UtcClock();
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantNameUpdateRequested.class, DefendantDetailUpdateRequested.class, DefendantDetailsUpdated.class, 
            DefendantAddressUpdateRequested.class, DefendantDateOfBirthUpdateRequested.class, 
            DefendantDetailsUpdateRequestAccepted.class);
    @InjectMocks
    private UpdateDefendantDetailsFromCCHandler updateDefendantDetailsFromCCHandler;
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
    public void shouldUpdateDefendantDetailsFromCC() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();

        final JsonEnvelope command = createUpdateDefendantDetailsFromCCCommand();

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        updateDefendantDetailsFromCCHandler.updateDefendantDetailsFromCC(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-name-update-requested"),
                                payloadIsJson(withJsonPath("$.newPersonalName.firstName", equalTo(FIRST_NAME)))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-detail-update-requested"),
                                payloadIsJson(withJsonPath("$.nameUpdated", is(true)))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.title", equalTo(defendant.getTitle())),
                                        withJsonPath("$.gender", equalTo(defendant.getGender().toString())),
                                        withJsonPath("$.nationalInsuranceNumber", equalTo(defendant.getNationalInsuranceNumber())),
                                        withJsonPath("$.contactDetails.email", equalTo(EMAIL)),
                                        withJsonPath("$.contactDetails.home", equalTo(defendant.getContactDetails().getHome())),
                                        withJsonPath("$.contactDetails.mobile", equalTo(defendant.getContactDetails().getMobile())),
                                        withJsonPath("$.region", equalTo(REGION)),
                                        withJsonPath("$.driverNumber", equalTo(DRIVER_NUMBER))))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-update-request-accepted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.newPersonalName.firstName", equalTo(FIRST_NAME)))))
                )));
    }

    @Test
    public void shouldUpdateDefendantDetailsFromCCWhenCaseIsCompleted() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();
        final JsonEnvelope command = createUpdateDefendantDetailsFromCCCommandWithUpdatedAddress();

        caseAggregate.getState().markCaseCompleted();

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        updateDefendantDetailsFromCCHandler.updateDefendantDetailsFromCC(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-address-update-requested"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.newAddress.address1", equalTo("Flat 2")),
                                        withJsonPath("$.newAddress.address2", equalTo("1 Oxford Road")),
                                        withJsonPath("$.newAddress.postcode", equalTo("RG2 8DS"))))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-detail-update-requested"),
                                payloadIsJson(withJsonPath("$.addressUpdated", is(true)))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updated"),
                                payloadIsJson(withJsonPath("$.defendantId", equalTo(defendantId.toString())))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-update-request-accepted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.newAddress.address1", equalTo("Flat 2")),
                                        withJsonPath("$.newAddress.postcode", equalTo("RG2 8DS")))))
                )));
    }

    @Test
    public void shouldUpdateDefendantDetailsFromCCWhenCaseIsReferredForCourtHearing() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();
        final JsonEnvelope command = createUpdateDefendantDetailsFromCCCommandWithUpdatedAddress();

        caseAggregate.getState().markCaseReferredForCourtHearing();

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        updateDefendantDetailsFromCCHandler.updateDefendantDetailsFromCC(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-address-update-requested"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.newAddress.address1", equalTo("Flat 2")),
                                        withJsonPath("$.newAddress.address2", equalTo("1 Oxford Road")),
                                        withJsonPath("$.newAddress.postcode", equalTo("RG2 8DS"))))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-detail-update-requested"),
                                payloadIsJson(withJsonPath("$.addressUpdated", is(true)))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updated"),
                                payloadIsJson(withJsonPath("$.defendantId", equalTo(defendantId.toString())))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-update-request-accepted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.newAddress.address1", equalTo("Flat 2")),
                                        withJsonPath("$.newAddress.postcode", equalTo("RG2 8DS")))))
                )));
    }

    private JsonEnvelope createUpdateDefendantDetailsFromCCCommand() {
        final Defendant defendant = caseReceivedEvent.getDefendant();
        final JsonObject contactNumber = createObjectBuilder()
                .add("home", defendant.getContactDetails().getHome())
                .add("mobile", defendant.getContactDetails().getMobile())
                .build();

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendant.getId().toString())
                .add("caseId", caseReceivedEvent.getCaseId().toString())
                .add("title", defendant.getTitle())
                .add("firstName", FIRST_NAME)
                .add("lastName", defendant.getLastName())
                .add("dateOfBirth", defendant.getDateOfBirth().format(ofPattern("yyyy-MM-dd")))
                .add("email", EMAIL)
                .add("gender", defendant.getGender().toString())
                .add("nationalInsuranceNumber", defendant.getNationalInsuranceNumber())
                .add("contactNumber", contactNumber)
                .add("address", toJsonObject(defendant.getAddress()))
                .add("region", REGION)
                .add("driverNumber", DRIVER_NUMBER);

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.update-defendant-details-from-CC"),
                payload.build());
    }

    private JsonEnvelope createUpdateDefendantDetailsFromCCCommandWithUpdatedAddress() {
        final Defendant defendant = caseReceivedEvent.getDefendant();
        final JsonObject contactNumber = createObjectBuilder()
                .add("home", defendant.getContactDetails().getHome())
                .add("mobile", defendant.getContactDetails().getMobile())
                .build();
        final Address updatedAddress = new Address("Flat 2","1 Oxford Road","","","","RG2 8DS");
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendant.getId().toString())
                .add("caseId", caseReceivedEvent.getCaseId().toString())
                .add("title", defendant.getTitle())
                .add("firstName", defendant.getFirstName())
                .add("lastName", defendant.getLastName())
                .add("dateOfBirth", defendant.getDateOfBirth().format(ofPattern("yyyy-MM-dd")))
                .add("email", defendant.getContactDetails().getEmail())
                .add("gender", defendant.getGender().toString())
                .add("nationalInsuranceNumber", defendant.getNationalInsuranceNumber())
                .add("contactNumber", contactNumber)
                .add("address", toJsonObject(updatedAddress))
                .add("region", defendant.getRegion())
                .add("driverNumber", defendant.getDriverNumber())
                .add("legalEntityName","legalEntityName");

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.update-defendant-details-from-CC"),
                payload.build());
    }


    @Test
    public void shouldUpdateLegalEntityDefendantDetailsFromCCWhenAddressChanged() throws EventStreamException {
        // given
        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();

        // Set up state: set legal entity name to match so only address change is detected
        caseAggregate.getState().setDefendantLegalEntityName("Acme Corporation Ltd");
        // Set address to be different from the new one
        final Address oldAddress = new Address("123 Old Street", "Old City", "", "", "", "SW1A 1AA");
        caseAggregate.getState().setDefendantAddress(oldAddress);

        final JsonEnvelope command = createUpdateLegalEntityDefendantDetailsFromCCCommandWithUpdatedAddress();

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        updateDefendantDetailsFromCCHandler.updateDefendantDetailsFromCC(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-address-update-requested"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.newAddress.address1", equalTo("789 New Street")),
                                        withJsonPath("$.newAddress.postcode", equalTo("SW1B 2CC"))))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-detail-update-requested"),
                                payloadIsJson(withJsonPath("$.addressUpdated", is(true)))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updated"),
                                payloadIsJson(withJsonPath("$.defendantId", equalTo(defendantId.toString())))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-update-request-accepted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.newAddress.address1", equalTo("789 New Street")),
                                        withJsonPath("$.newAddress.postcode", equalTo("SW1B 2CC")))))
                )));
    }



    private JsonEnvelope createUpdateLegalEntityDefendantDetailsFromCCCommandWithUpdatedAddress() {
        final Defendant defendant = caseReceivedEvent.getDefendant();
        final JsonObject contactDetails = createObjectBuilder()
                .add("email", "contact@acme.com")
                .build();

        final JsonObject updatedAddress = createObjectBuilder()
                .add("address1", "789 New Street")
                .add("address2", "New City")
                .add("postcode", "SW1B 2CC")
                .build();

        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("name", "Acme Corporation Ltd")
                .add("address", updatedAddress)
                .add("contactDetails", contactDetails)
                .build();

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendant.getId().toString())
                .add("caseId", caseReceivedEvent.getCaseId().toString())
                .add("legalEntityDefendant", legalEntityDefendant);

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.update-defendant-details-from-CC"),
                payload.build());
    }

}

