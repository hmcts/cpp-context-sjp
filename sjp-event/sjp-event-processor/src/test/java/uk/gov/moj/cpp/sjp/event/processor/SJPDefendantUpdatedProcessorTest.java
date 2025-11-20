package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SJPDefendantUpdatedProcessorTest {

    @InjectMocks
    private SJPDefendantUpdatedProcessor processor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldSendUpdateDefendantDetailsFromCCCommandWhenDefendantChanged() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("title", "MR")
                .add("firstName", "John")
                .add("lastName", "Doe")
                .add("dateOfBirth", "1980-01-01")
                .add("gender", "MALE")
                .add("nationalInsuranceNumber", "AB123456C")
                .add("region", "London")
                .add("address", createObjectBuilder()
                        .add("address1", "123 Test Street")
                        .add("address2", "Test City")
                        .add("address3", "Test County")
                        .add("address4", "UK")
                        .add("address5", "Greater London")
                        .add("postcode", "SW1A 1AA")
                        .build())
                .add("contact", createObjectBuilder()
                        .add("home", "02012345678")
                        .add("mobile", "07123456789")
                        .add("work", "02087654321")
                        .add("primaryEmail", "john.doe@example.com")
                        .add("secondaryEmail", "john.doe.secondary@example.com")
                        .build())
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .add("driverNumber", "TEST123456")
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-123")
                .add("prosecutionCaseId", "case-id-456")
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope commandEnvelope = envelopeCaptor.getValue();
        assertThat(commandEnvelope.metadata().name(), is("sjp.command.update-defendant-details-from-CC"));
        
        final JsonObject commandPayload = commandEnvelope.payloadAsJsonObject();
        assertThat(commandPayload.getString("caseId"), is("case-id-456"));
        assertThat(commandPayload.getString("defendantId"), is("defendant-id-123"));
        assertThat(commandPayload.getString("title"), is("MR"));
        assertThat(commandPayload.getString("firstName"), is("John"));
        assertThat(commandPayload.getString("lastName"), is("Doe"));
        assertThat(commandPayload.getString("dateOfBirth"), is("1980-01-01"));
        assertThat(commandPayload.getString("gender"), is("Male"));
        assertThat(commandPayload.getString("nationalInsuranceNumber"), is("AB123456C"));
        assertThat(commandPayload.getString("region"), is("London"));
        assertThat(commandPayload.getString("driverNumber"), is("TEST123456"));
        assertThat(commandPayload.getString("email"), is("john.doe@example.com"));
        assertThat(commandPayload.getString("email2"), is("john.doe.secondary@example.com"));
        
        final JsonObject contactNumber = commandPayload.getJsonObject("contactNumber");
        assertThat(contactNumber.getString("home"), is("02012345678"));
        assertThat(contactNumber.getString("mobile"), is("07123456789"));
        assertThat(contactNumber.getString("business"), is("02087654321"));
        
        final JsonObject address = commandPayload.getJsonObject("address");
        assertThat(address.getString("address1"), is("123 Test Street"));
        assertThat(address.getString("address2"), is("Test City"));
        assertThat(address.getString("postcode"), is("SW1A 1AA"));
    }

    @Test
    public void shouldNotSendCommandWhenDefendantIsNull() {
        // given
        final JsonObject eventPayload = createObjectBuilder().build();
        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldNotSendCommandWhenPersonDefendantIsNull() {
        // given
        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-123")
                .add("prosecutionCaseId", "case-id-456")
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldNotSendCommandWhenDefendantIdIsMissing() {
        // given
        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", createObjectBuilder().build())
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("prosecutionCaseId", "case-id-456")
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldNotSendCommandWhenProsecutionCaseIdIsMissing() {
        // given
        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", createObjectBuilder().build())
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-123")
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldHandleAddressFromDefendantLevelWhenNotInPersonDetails() {
        // given
        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", createObjectBuilder().build())
                .build();

        final JsonObject address = createObjectBuilder()
                .add("address1", "123 Test Street")
                .add("address2", "Test City")
                .add("postcode", "SW1A 1AA")
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-123")
                .add("prosecutionCaseId", "case-id-456")
                .add("personDefendant", personDefendant)
                .add("address", address)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.getJsonObject("address").getString("address1"), is("123 Test Street"));
        assertThat(commandPayload.getJsonObject("address").getString("postcode"), is("SW1A 1AA"));
    }

    @Test
    public void shouldHandleMinimalPersonDetails() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Jane")
                .add("lastName", "Smith")
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-789")
                .add("prosecutionCaseId", "case-id-012")
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.getString("caseId"), is("case-id-012"));
        assertThat(commandPayload.getString("defendantId"), is("defendant-id-789"));
        assertThat(commandPayload.getString("firstName"), is("Jane"));
        assertThat(commandPayload.getString("lastName"), is("Smith"));
    }

    @Test
    public void shouldHandleContactNumberWithOnlyHomeNumber() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .add("contact", createObjectBuilder()
                        .add("home", "02011111111")
                        .build())
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-111")
                .add("prosecutionCaseId", "case-id-222")
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        final JsonObject contactNumber = commandPayload.getJsonObject("contactNumber");
        assertThat(contactNumber.getString("home"), is("02011111111"));
    }

    @Test
    public void shouldHandleEmptyContactNumber() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .add("contact", createObjectBuilder().build())
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-333")
                .add("prosecutionCaseId", "case-id-444")
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        // contactNumber should not be present if empty
        assertThat(commandPayload.containsKey("contactNumber"), is(false));
    }

    @Test
    public void shouldHandleNullValuesInPersonDetails() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .addNull("title")
                .addNull("dateOfBirth")
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-555")
                .add("prosecutionCaseId", "case-id-666")
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.getString("firstName"), is("Test"));
        assertThat(commandPayload.getString("lastName"), is("User"));
        // null values should not be added to payload
        assertThat(commandPayload.containsKey("title"), is(false));
        assertThat(commandPayload.containsKey("dateOfBirth"), is(false));
    }

    @Test
    public void shouldPreserveMetadataFromOriginalEvent() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-777")
                .add("prosecutionCaseId", "case-id-888")
                .add("personDefendant", personDefendant)
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", defendant)
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // when
        processor.handleCaseDefendantChanged(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope commandEnvelope = envelopeCaptor.getValue();
        assertThat(commandEnvelope.metadata().name(), is("sjp.command.update-defendant-details-from-CC"));
        // Metadata should be preserved from original event
        assertThat(commandEnvelope.metadata().id(), is(eventEnvelope.metadata().id()));
    }
}

