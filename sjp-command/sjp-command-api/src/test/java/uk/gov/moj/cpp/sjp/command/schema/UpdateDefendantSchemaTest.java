package uk.gov.moj.cpp.sjp.command.schema;

import static javax.json.Json.createReader;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Ignore("Those tests can be looked when the JsonSchemaValidationMatcher supports the Json catalogues...")
public class UpdateDefendantSchemaTest {

    @Test
    public void acceptsValidEnvelopeWithMrTitle() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/validWithMrTitle.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void acceptsValidEnvelopeWithLowercasePostcodeDoesNotContainingSpace() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/validLowercasePostcodeWithoutSpace.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void acceptsValidEnvelopeWithMrsTitle() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/validWithMrsTitle.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void acceptsValidEnvelopeWithCoTitle() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/validWithCoTitle.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void acceptsValidEnvelopeWithMissTitle() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/validWithMissTitle.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void acceptsValidEnvelopeWithMsTitle() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/validWithMsTitle.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void rejectsWhenFirstNameIsMissing() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/missingFirstName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        expectedException.expectMessage("required key [firstName] not found");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void rejectsWhenLastNameIsMissing() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/missingLastName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        expectedException.expectMessage("required key [lastName] not found");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void rejectsWhenFirstNameIsBlank() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/emptyFirstName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        expectedException.expectMessage("#/firstName: expected minLength: 1, actual: 0");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void rejectsWhenLastNameIsBlank() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/emptyLastName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        expectedException.expectMessage("#/lastName: expected minLength: 1, actual: 0");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void rejectsWhenFirstNameIsNull() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/nullFirstName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        expectedException.expectMessage("firstName: expected type: String, found: Null");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void rejectsWhenLastNameIsNull() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/nullLastName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        expectedException.expectMessage("lastName: expected type: String, found: Null");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void rejectsWhenFirstNameIsTooLong() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/tooLongFirstName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        expectedException.expectMessage("firstName: expected maxLength: 255, actual: 256");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void rejectsWhenLastNameIsTooLong() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/tooLongLastName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        expectedException.expectMessage("lastName: expected maxLength: 255, actual: 256");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    private JsonObject readPayloadFromResource(String path) {
        return createReader(UpdateDefendantSchemaTest.class.getResourceAsStream(path))
                .readObject();
    }


}
