package uk.gov.moj.cpp.sjp.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
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
public class DefendantDetailsUpdateRequestAcceptedProcessorTest {

    @InjectMocks
    private DefendantDetailsUpdateRequestAcceptedProcessor processor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldSendAcceptPendingDefendantChangesCommandWhenAllFieldsChanged() {
        // given
        final JsonObject newPersonalName = createObjectBuilder()
                .add("title", "Mr")
                .add("firstName", "John")
                .add("lastName", "Doe")
                .build();

        final JsonObject newAddress = createObjectBuilder()
                .add("address1", "123 Test Street")
                .add("address2", "Test City")
                .add("address3", "Test County")
                .add("address4", "UK")
                .add("address5", "Greater London")
                .add("postcode", "SW1A 1AA")
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", "case-id-123")
                .add("defendantId", "defendant-id-456")
                .add("newPersonalName", newPersonalName)
                .add("newAddress", newAddress)
                .add("newDateOfBirth", "1990-05-15")
                .add("updatedAt", "2018-02-05T15:14:29.894Z")
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-details-update-request-accepted"),
                eventPayload);

        // when
        processor.handleDefendantDetailsUpdateRequestAccepted(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope commandEnvelope = envelopeCaptor.getValue();
        assertThat(commandEnvelope.metadata().name(), is("sjp.command.accept-pending-defendant-changes-from-CC"));
        
        final JsonObject commandPayload = commandEnvelope.payloadAsJsonObject();
        assertThat(commandPayload.getString("caseId"), is("case-id-123"));
        assertThat(commandPayload.getString("defendantId"), is("defendant-id-456"));
        assertThat(commandPayload.getString("firstName"), is("John"));
        assertThat(commandPayload.getString("lastName"), is("Doe"));
        assertThat(commandPayload.getString("dateOfBirth"), is("1990-05-15"));
        
        final JsonObject address = commandPayload.getJsonObject("address");
        assertThat(address.getString("address1"), is("123 Test Street"));
        assertThat(address.getString("address2"), is("Test City"));
        assertThat(address.getString("postcode"), is("SW1A 1AA"));
    }

    @Test
    public void shouldSendAcceptPendingDefendantChangesCommandWhenOnlyNameChanged() {
        // given
        final JsonObject newPersonalName = createObjectBuilder()
                .add("title", "Mrs")
                .add("firstName", "Jane")
                .add("lastName", "Smith")
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", "case-id-789")
                .add("defendantId", "defendant-id-012")
                .add("newPersonalName", newPersonalName)
                .add("updatedAt", "2018-02-05T15:14:29.894Z")
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-details-update-request-accepted"),
                eventPayload);

        // when
        processor.handleDefendantDetailsUpdateRequestAccepted(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.getString("caseId"), is("case-id-789"));
        assertThat(commandPayload.getString("defendantId"), is("defendant-id-012"));
        assertThat(commandPayload.getString("firstName"), is("Jane"));
        assertThat(commandPayload.getString("lastName"), is("Smith"));
        assertThat(commandPayload.containsKey("dateOfBirth"), is(false));
        assertThat(commandPayload.containsKey("address"), is(false));
    }

    @Test
    public void shouldSendAcceptPendingDefendantChangesCommandWhenOnlyAddressChanged() {
        // given
        final JsonObject newAddress = createObjectBuilder()
                .add("address1", "456 New Street")
                .add("address2", "New City")
                .add("postcode", "M1 1AA")
                .build();

        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", "case-id-345")
                .add("defendantId", "defendant-id-678")
                .add("newAddress", newAddress)
                .add("updatedAt", "2018-02-05T15:14:29.894Z")
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-details-update-request-accepted"),
                eventPayload);

        // when
        processor.handleDefendantDetailsUpdateRequestAccepted(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.getString("caseId"), is("case-id-345"));
        assertThat(commandPayload.getString("defendantId"), is("defendant-id-678"));
        assertThat(commandPayload.containsKey("firstName"), is(false));
        assertThat(commandPayload.containsKey("lastName"), is(false));
        assertThat(commandPayload.containsKey("dateOfBirth"), is(false));
        
        final JsonObject address = commandPayload.getJsonObject("address");
        assertThat(address.getString("address1"), is("456 New Street"));
        assertThat(address.getString("postcode"), is("M1 1AA"));
    }

    @Test
    public void shouldSendAcceptPendingDefendantChangesCommandWhenOnlyDateOfBirthChanged() {
        // given
        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", "case-id-111")
                .add("defendantId", "defendant-id-222")
                .add("newDateOfBirth", "1985-03-20")
                .add("updatedAt", "2018-02-05T15:14:29.894Z")
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-details-update-request-accepted"),
                eventPayload);

        // when
        processor.handleDefendantDetailsUpdateRequestAccepted(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.getString("caseId"), is("case-id-111"));
        assertThat(commandPayload.getString("defendantId"), is("defendant-id-222"));
        assertThat(commandPayload.getString("dateOfBirth"), is("1985-03-20"));
        assertThat(commandPayload.containsKey("firstName"), is(false));
        assertThat(commandPayload.containsKey("lastName"), is(false));
        assertThat(commandPayload.containsKey("address"), is(false));
    }

    @Test
    public void shouldSendAcceptPendingDefendantChangesCommandWhenLegalEntityNameChanged() {
        // given
        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", "case-id-333")
                .add("defendantId", "defendant-id-444")
                .add("newLegalEntityName", "Test Company Ltd")
                .add("updatedAt", "2018-02-05T15:14:29.894Z")
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-details-update-request-accepted"),
                eventPayload);

        // when
        processor.handleDefendantDetailsUpdateRequestAccepted(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.getString("caseId"), is("case-id-333"));
        assertThat(commandPayload.getString("defendantId"), is("defendant-id-444"));
        assertThat(commandPayload.getString("legalEntityName"), is("Test Company Ltd"));
    }

    @Test
    public void shouldNotSendCommandWhenCaseIdIsMissing() {
        // given
        final JsonObject eventPayload = createObjectBuilder()
                .add("defendantId", "defendant-id-555")
                .add("newDateOfBirth", "1990-01-01")
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-details-update-request-accepted"),
                eventPayload);

        // when
        processor.handleDefendantDetailsUpdateRequestAccepted(eventEnvelope);

        // then
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldNotSendCommandWhenDefendantIdIsMissing() {
        // given
        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", "case-id-666")
                .add("newDateOfBirth", "1990-01-01")
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-details-update-request-accepted"),
                eventPayload);

        // when
        processor.handleDefendantDetailsUpdateRequestAccepted(eventEnvelope);

        // then
        verify(sender, never()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldPreserveMetadataFromOriginalEvent() {
        // given
        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", "case-id-777")
                .add("defendantId", "defendant-id-888")
                .add("newDateOfBirth", "1990-01-01")
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-details-update-request-accepted"),
                eventPayload);

        // when
        processor.handleDefendantDetailsUpdateRequestAccepted(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope commandEnvelope = envelopeCaptor.getValue();
        assertThat(commandEnvelope.metadata().name(), is("sjp.command.accept-pending-defendant-changes-from-CC"));
        // Metadata should be preserved from original event
        assertThat(commandEnvelope.metadata().id(), is(eventEnvelope.metadata().id()));
    }

    @Test
    public void shouldHandleNullValuesInEventPayload() {
        // given
        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", "case-id-999")
                .add("defendantId", "defendant-id-000")
                .addNull("newPersonalName")
                .addNull("newAddress")
                .addNull("newDateOfBirth")
                .addNull("newLegalEntityName")
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-details-update-request-accepted"),
                eventPayload);

        // when
        processor.handleDefendantDetailsUpdateRequestAccepted(eventEnvelope);

        // then
        verify(sender).send(envelopeCaptor.capture());
        final JsonObject commandPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(commandPayload.getString("caseId"), is("case-id-999"));
        assertThat(commandPayload.getString("defendantId"), is("defendant-id-000"));
        // Null values should not be added to payload
        assertThat(commandPayload.containsKey("firstName"), is(false));
        assertThat(commandPayload.containsKey("lastName"), is(false));
        assertThat(commandPayload.containsKey("address"), is(false));
        assertThat(commandPayload.containsKey("dateOfBirth"), is(false));
        assertThat(commandPayload.containsKey("legalEntityName"), is(false));
    }
}

