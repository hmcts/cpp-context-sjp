package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
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

import uk.gov.justice.json.schemas.domains.sjp.Gender;
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
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDetailUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdateRequested;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantDetailsHandlerTest extends CaseAggregateBaseTest {

    private static final UUID defendantId = randomUUID();
    private static final UUID caseId = randomUUID();
    private static final String firstName = "test";
    private static final String lastName = "lastName";
    private static final String email = "email";
    private static final String mobileNumber = "mobileNumber";
    private static final Gender gender = Gender.MALE;
    private static final String nationalInsuranceNumber = "nationalInsuranceNumber";
    private static final String homeNumber = "homeNumber";
    private static final String dateOfBirth = LocalDate.parse("1980-07-15").toString();
    private static final String ADDRESS_1 = "14 Tottenham Court Road";
    private static final String ADDRESS_2 = "London";
    private static final String ADDRESS_3 = "Surrey";
    private static final String ADDRESS_4 = "England";
    private static final String ADDRESS_5 = "United Kingdom";
    private static final String POSTCODE = "W1T 1JY";
    private static final Address ADDRESS = new Address(ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, ADDRESS_5, POSTCODE);
    private static final String REGION = "REGION";
    private static final String DRIVER_NUMBER = "MORGA753116SM9IJ";
    @Spy
    private Clock clock = new UtcClock();
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantNameUpdateRequested.class, DefendantDetailUpdateRequested.class, DefendantDetailsUpdated.class, DefendantAddressUpdateRequested.class);
    @InjectMocks
    private UpdateDefendantDetailsHandler updateDefendantDetailsHandler;
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
    public void shouldUpdateDefendantDetails() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();

        final JsonEnvelope command = createUpdateDefendantDetailsCommand(randomUUID());

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        updateDefendantDetailsHandler.updateDefendantDetails(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-name-update-requested"),
                                payloadIsJson(withJsonPath("$.newPersonalName.firstName", equalTo(firstName)))),
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
                                        withJsonPath("$.contactDetails.email", equalTo(email)),
                                        withJsonPath("$.contactDetails.home", equalTo(defendant.getContactDetails().getHome())),
                                        withJsonPath("$.contactDetails.mobile", equalTo(defendant.getContactDetails().getMobile())),
                                        withJsonPath("$.region", equalTo(REGION)),
                                        withJsonPath("$.driverNumber", equalTo(DRIVER_NUMBER)))))
                )));
    }

    @Test
    public void shouldUpdateDefendantDetailsForCompletedCase_commandReceivedFromApplication() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();
        final JsonEnvelope command = createUpdateDefendantDetailsCommandWithUpdatedAddress(randomUUID());

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        updateDefendantDetailsHandler.updateDefendantDetails(command);

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
                                payloadIsJson(allOf(
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.address.address1", equalTo("Flat 2")),
                                        withJsonPath("$.address.address2", equalTo("1 Oxford Road")),
                                        withJsonPath("$.address.postcode", equalTo("RG2 8DS")))))
                )));
    }

    private JsonEnvelope createUpdateDefendantDetailsCommand(UUID userId) {
        final Defendant defendant = caseReceivedEvent.getDefendant();
        final JsonObject contactNumber = createObjectBuilder()
                .add("home", defendant.getContactDetails().getHome())
                .add("mobile", defendant.getContactDetails().getMobile())
                .build();

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendant.getId().toString())
                .add("caseId", caseReceivedEvent.getCaseId().toString())
                .add("title", defendant.getTitle())
                .add("firstName", firstName)
                .add("lastName", defendant.getLastName())
                .add("dateOfBirth", defendant.getDateOfBirth().format(ofPattern("YYY-MM-dd")))
                .add("email", email)
                .add("gender", defendant.getGender().toString())
                .add("nationalInsuranceNumber", defendant.getNationalInsuranceNumber())
                .add("contactNumber", contactNumber)
                .add("address", toJsonObject(defendant.getAddress()))
                .add("region", REGION)
                .add("driverNumber", DRIVER_NUMBER);

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.update-defendant-details").withUserId(userId.toString()),
                payload.build());
    }

    private JsonEnvelope createUpdateDefendantDetailsCommandWithUpdatedAddress(UUID userId) {
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
                .add("dateOfBirth", defendant.getDateOfBirth().format(ofPattern("YYY-MM-dd")))
                .add("email", defendant.getContactDetails().getEmail())
                .add("gender", defendant.getGender().toString())
                .add("nationalInsuranceNumber", defendant.getNationalInsuranceNumber())
                .add("contactNumber", contactNumber)
                .add("address", toJsonObject(updatedAddress))
                .add("region", defendant.getRegion())
                .add("driverNumber", defendant.getDriverNumber())
                .add("addressUpdateFromApplication","true")
                .add("legalEntityName","legalEntityName");

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.update-defendant-details").withUserId(userId.toString()),
                payload.build());
    }

}