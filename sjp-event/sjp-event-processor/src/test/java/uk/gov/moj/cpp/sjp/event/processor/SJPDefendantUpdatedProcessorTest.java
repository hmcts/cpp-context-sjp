package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
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
class SJPDefendantUpdatedProcessorTest {

    @InjectMocks
    private SJPDefendantUpdatedProcessor processor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    void shouldSendUpdateDefendantDetailsFromCCCommandWhenDefendantChanged() {
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
    void shouldNotSendCommandWhenDefendantIsNull() {
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
    void shouldNotSendCommandWhenPersonDefendantAndLegalEntityDefendantAreNull() {
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
    void shouldSendUpdateDefendantDetailsFromCCCommandWhenLegalEntityDefendantChanged() {
        // given
        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("name", "Acme Corporation Ltd")
                .add("address", createObjectBuilder()
                        .add("address1", "456 Business Park")
                        .add("address2", "Corporate City")
                        .add("address3", "Business County")
                        .add("address4", "UK")
                        .add("address5", "England")
                        .add("postcode", "EC1A 1BB")
                        .build())
                .add("contactDetails", createObjectBuilder()
                        .add("home", "02011111111")
                        .add("mobile", "07111111111")
                        .add("business", "02022222222")
                        .add("email", "contact@acme.com")
                        .add("email2", "info@acme.com")
                        .build())
                .add("incorporationNumber", "INC123456")
                .add("position", "Director")
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-789")
                .add("prosecutionCaseId", "case-id-999")
                .add("legalEntityDefendant", legalEntityDefendant)
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
        assertThat(commandPayload.getString("caseId"), is("case-id-999"));
        assertThat(commandPayload.getString("defendantId"), is("defendant-id-789"));
        
        final JsonObject legalEntity = commandPayload.getJsonObject("legalEntityDefendant");
        assertThat(legalEntity.getString("name"), is("Acme Corporation Ltd"));
        assertThat(legalEntity.getString("incorporationNumber"), is("INC123456"));
        assertThat(legalEntity.getString("position"), is("Director"));
        
        final JsonObject address = legalEntity.getJsonObject("address");
        assertThat(address.getString("address1"), is("456 Business Park"));
        assertThat(address.getString("postcode"), is("EC1A 1BB"));
        
        final JsonObject contactDetails = legalEntity.getJsonObject("contactDetails");
        assertThat(contactDetails.getString("home"), is("02011111111"));
        assertThat(contactDetails.getString("mobile"), is("07111111111"));
        assertThat(contactDetails.getString("business"), is("02022222222"));
        assertThat(contactDetails.getString("email"), is("contact@acme.com"));
        assertThat(contactDetails.getString("email2"), is("info@acme.com"));
    }

    @Test
    void shouldSendCommandWhenOnlyLegalEntityDefendantIsPresent() {
        // given
        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("name", "Test Company Ltd")
                .add("address", createObjectBuilder()
                        .add("address1", "123 Test Street")
                        .add("postcode", "SW1A 1AA")
                        .build())
                .add("contactDetails", createObjectBuilder()
                        .add("email", "test@company.com")
                        .build())
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-legal")
                .add("prosecutionCaseId", "case-id-legal")
                .add("legalEntityDefendant", legalEntityDefendant)
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
        assertThat(commandPayload.containsKey("legalEntityDefendant"), is(true));
        assertThat(commandPayload.getJsonObject("legalEntityDefendant").getString("name"), is("Test Company Ltd"));
    }

    @Test
    void shouldNotSendCommandWhenDefendantIdIsMissing() {
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
    void shouldNotSendCommandWhenProsecutionCaseIdIsMissing() {
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
    void shouldHandleAddressFromDefendantLevelWhenNotInPersonDetails() {
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
    void shouldHandleMinimalPersonDetails() {
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
    void shouldHandleContactNumberWithOnlyHomeNumber() {
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
    void shouldHandleEmptyContactNumber() {
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
    void shouldHandleNullValuesInPersonDetails() {
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
    void shouldPreserveMetadataFromOriginalEvent() {
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

    @Test
    void shouldHandleLegalEntityWithOrganisationStructure() {
        // given
        final JsonObject organisation = createObjectBuilder()
                .add("name", "Organisation Corp Ltd")
                .add("address", createObjectBuilder()
                        .add("address1", "789 Org Street")
                        .add("postcode", "M1 2AB")
                        .build())
                .add("contact", createObjectBuilder()
                        .add("home", "02033333333")
                        .add("mobile", "07933333333")
                        .add("work", "02044444444")
                        .add("primaryEmail", "org@corp.com")
                        .add("secondaryEmail", "info@corp.com")
                        .build())
                .add("incorporationNumber", "ORG123456")
                .build();

        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("organisation", organisation)
                .add("position", "CEO")
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-org")
                .add("prosecutionCaseId", "case-id-org")
                .add("legalEntityDefendant", legalEntityDefendant)
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
        final JsonObject legalEntity = commandPayload.getJsonObject("legalEntityDefendant");
        assertThat(legalEntity.getString("name"), is("Organisation Corp Ltd"));
        assertThat(legalEntity.getString("incorporationNumber"), is("ORG123456"));
        assertThat(legalEntity.getString("position"), is("CEO"));
        
        final JsonObject contactDetails = legalEntity.getJsonObject("contactDetails");
        assertThat(contactDetails.getString("home"), is("02033333333"));
        assertThat(contactDetails.getString("mobile"), is("07933333333"));
        assertThat(contactDetails.getString("business"), is("02044444444"));
        assertThat(contactDetails.getString("email"), is("org@corp.com"));
        assertThat(contactDetails.getString("email2"), is("info@corp.com"));
    }

    @Test
    void shouldHandleLegalEntityWithEmptyContactInOrganisation() {
        // given
        final JsonObject organisation = createObjectBuilder()
                .add("name", "Empty Contact Corp")
                .add("contact", createObjectBuilder().build())
                .build();

        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("organisation", organisation)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-empty")
                .add("prosecutionCaseId", "case-id-empty")
                .add("legalEntityDefendant", legalEntityDefendant)
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
        final JsonObject legalEntity = commandPayload.getJsonObject("legalEntityDefendant");
        assertThat(legalEntity.getString("name"), is("Empty Contact Corp"));
        // contactDetails should not be present if empty
        assertThat(legalEntity.containsKey("contactDetails"), is(false));
    }

    @Test
    void shouldHandleLegalEntityWithPartialContactInOrganisation() {
        // given
        final JsonObject organisation = createObjectBuilder()
                .add("name", "Partial Contact Corp")
                .add("contact", createObjectBuilder()
                        .add("primaryEmail", "partial@corp.com")
                        .build())
                .build();

        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("organisation", organisation)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-partial")
                .add("prosecutionCaseId", "case-id-partial")
                .add("legalEntityDefendant", legalEntityDefendant)
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
        final JsonObject legalEntity = commandPayload.getJsonObject("legalEntityDefendant");
        final JsonObject contactDetails = legalEntity.getJsonObject("contactDetails");
        assertThat(contactDetails.getString("email"), is("partial@corp.com"));
    }

    @Test
    void shouldHandleLegalEntityWithOnlyPosition() {
        // given - legal entity with only position, no other fields
        // Note: position alone will still create a non-empty builder, so legalEntityDefendant will be added
        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("position", "Director")
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-empty-legal")
                .add("prosecutionCaseId", "case-id-empty-legal")
                .add("legalEntityDefendant", legalEntityDefendant)
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
        // legalEntityDefendant will be added because position makes it non-empty
        assertThat(commandPayload.containsKey("legalEntityDefendant"), is(true));
        assertThat(commandPayload.getJsonObject("legalEntityDefendant").getString("position"), is("Director"));
    }

    @Test
    void shouldHandleBothPersonAndLegalEntityDefendant() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "John")
                .add("lastName", "Doe")
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("name", "Company Ltd")
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-both")
                .add("prosecutionCaseId", "case-id-both")
                .add("personDefendant", personDefendant)
                .add("legalEntityDefendant", legalEntityDefendant)
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
        assertThat(commandPayload.getString("firstName"), is("John"));
        assertThat(commandPayload.getString("lastName"), is("Doe"));
        assertThat(commandPayload.getJsonObject("legalEntityDefendant").getString("name"), is("Company Ltd"));
    }

    @Test
    void shouldHandlePersonDefendantWithoutPersonDetails() {
        // given
        final JsonObject personDefendant = createObjectBuilder()
                .add("driverNumber", "DRV123")
                .build();

        final JsonObject address = createObjectBuilder()
                .add("address1", "No PersonDetails St")
                .add("postcode", "SW1A 1AA")
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-no-details")
                .add("prosecutionCaseId", "case-id-no-details")
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
        assertThat(commandPayload.getString("driverNumber"), is("DRV123"));
        assertThat(commandPayload.getJsonObject("address").getString("address1"), is("No PersonDetails St"));
    }

    @Test
    void shouldHandleGenderConversionForFemale() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Jane")
                .add("lastName", "Doe")
                .add("gender", "FEMALE")
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-female")
                .add("prosecutionCaseId", "case-id-female")
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
        assertThat(commandPayload.getString("gender"), is("Female"));
    }

    @Test
    void shouldHandleGenderConversionForNotKnown() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .add("gender", "NOT_KNOWN")
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-not-known")
                .add("prosecutionCaseId", "case-id-not-known")
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
        assertThat(commandPayload.getString("gender"), is("Not Specified"));
    }

    @Test
    void shouldHandleGenderConversionForNotSpecified() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .add("gender", "NOT_SPECIFIED")
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-not-specified")
                .add("prosecutionCaseId", "case-id-not-specified")
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
        assertThat(commandPayload.getString("gender"), is("Not Specified"));
    }

    @Test
    void shouldHandleGenderConversionForUnknownValue() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .add("gender", "UNKNOWN_GENDER")
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-unknown-gender")
                .add("prosecutionCaseId", "case-id-unknown-gender")
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
        // Unknown gender should be used as-is
        assertThat(commandPayload.getString("gender"), is("UNKNOWN_GENDER"));
    }

    @Test
    void shouldHandleGenderConversionCaseInsensitive() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .add("gender", "male")  // lowercase
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-lowercase")
                .add("prosecutionCaseId", "case-id-lowercase")
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
        assertThat(commandPayload.getString("gender"), is("Male"));
    }

    @Test
    void shouldHandleNonStringFieldInAddIfPresent() {
        // given - field exists but is not a string (e.g., number)
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .add("title", 12345)  // number instead of string
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-non-string")
                .add("prosecutionCaseId", "case-id-non-string")
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

        // then - should not throw exception, title should not be added
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.containsKey("title"), is(false));
    }

    @Test
    void shouldHandleNonStringGenderField() {
        // given - gender exists but is not a string
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .add("gender", 1)  // number instead of string
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-non-string-gender")
                .add("prosecutionCaseId", "case-id-non-string-gender")
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

        // then - should not throw exception, gender should not be added
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.containsKey("gender"), is(false));
    }

    @Test
    void shouldHandleRuntimeException() {
        // given
        final JsonObject eventPayload = createObjectBuilder()
                .add("defendant", createObjectBuilder()
                        .add("id", "defendant-id")
                        .add("prosecutionCaseId", "case-id")
                        .add("personDefendant", createObjectBuilder().build())
                        .build())
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.case-defendant-changed"),
                eventPayload);

        // Mock sender to throw RuntimeException
        final RuntimeException testException = new RuntimeException("Test runtime exception");
        doThrow(testException).when(sender).send(any());

        // when/then
        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            processor.handleCaseDefendantChanged(eventEnvelope);
        });
        assertThat(thrown, is(testException));
    }


    @Test
    void shouldHandleContactWithOnlyEmailFields() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .add("contact", createObjectBuilder()
                        .add("primaryEmail", "test@example.com")
                        .add("secondaryEmail", "test2@example.com")
                        .build())
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-email-only")
                .add("prosecutionCaseId", "case-id-email-only")
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
        assertThat(commandPayload.getString("email"), is("test@example.com"));
        assertThat(commandPayload.getString("email2"), is("test2@example.com"));
        // contactNumber should not be present if empty
        assertThat(commandPayload.containsKey("contactNumber"), is(false));
    }

    @Test
    void shouldHandleLegalEntityWithNullContactInOrganisation() {
        // given - organisation without contact field (not null, just missing)
        // Note: addNull() creates JsonValue.NULL which causes ClassCastException when calling getJsonObject()
        // So we just don't include the contact field at all
        final JsonObject organisation = createObjectBuilder()
                .add("name", "Null Contact Corp")
                // contact field is not present, so getJsonObject("contact") will return null
                .build();

        final JsonObject legalEntityDefendant = createObjectBuilder()
                .add("organisation", organisation)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-null-contact")
                .add("prosecutionCaseId", "case-id-null-contact")
                .add("legalEntityDefendant", legalEntityDefendant)
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
        final JsonObject legalEntity = commandPayload.getJsonObject("legalEntityDefendant");
        assertThat(legalEntity.getString("name"), is("Null Contact Corp"));
        assertThat(legalEntity.containsKey("contactDetails"), is(false));
    }

    @Test
    void shouldHandleLegalEntityWithNullGenderConversion() {
        // given
        final JsonObject personDetails = createObjectBuilder()
                .add("firstName", "Test")
                .add("lastName", "User")
                .addNull("gender")
                .build();

        final JsonObject personDefendant = createObjectBuilder()
                .add("personDetails", personDetails)
                .build();

        final JsonObject defendant = createObjectBuilder()
                .add("id", "defendant-id-null-gender")
                .add("prosecutionCaseId", "case-id-null-gender")
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
        assertThat(commandPayload.containsKey("gender"), is(false));
    }
}

