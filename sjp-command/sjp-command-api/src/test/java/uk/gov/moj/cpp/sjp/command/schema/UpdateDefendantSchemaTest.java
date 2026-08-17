package uk.gov.moj.cpp.sjp.command.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Those tests can be looked when the JsonSchemaValidationMatcher supports the Json catalogues...")
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

    @Test
    public void acceptsWhenValidDriverNumber() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/validDriverNumber.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }


    @Test
    public void rejectsWhenFirstNameIsMissing() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/missingFirstName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        var e = assertThrows(Exception.class, () -> assertThat(envelope, jsonEnvelope().thatMatchesSchema()));
        assertThat(e.getMessage(), is("required key [firstName] not found"));
    }

    @Test
    public void rejectsWhenLastNameIsMissing() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/missingLastName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        var e = assertThrows(Exception.class, () -> assertThat(envelope, jsonEnvelope().thatMatchesSchema()));
        assertThat(e.getMessage(), is("required key [lastName] not found"));
    }

    @Test
    public void rejectsWhenFirstNameIsBlank() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/emptyFirstName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        var e = assertThrows(Exception.class, () -> assertThat(envelope, jsonEnvelope().thatMatchesSchema()));
        assertThat(e.getMessage(), is("#/firstName: expected minLength: 1, actual: 0"));
    }

    @Test
    public void rejectsWhenLastNameIsBlank() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/emptyLastName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        var e = assertThrows(Exception.class, () -> assertThat(envelope, jsonEnvelope().thatMatchesSchema()));
        assertThat(e.getMessage(), is("#/lastName: expected minLength: 1, actual: 0"));
    }

    @Test
    public void rejectsWhenFirstNameIsNull() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/nullFirstName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        var e = assertThrows(Exception.class, () -> assertThat(envelope, jsonEnvelope().thatMatchesSchema()));
        assertThat(e.getMessage(), is("firstName: expected type: String, found: Null"));
    }

    @Test
    public void rejectsWhenLastNameIsNull() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/nullLastName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        var e = assertThrows(Exception.class, () -> assertThat(envelope, jsonEnvelope().thatMatchesSchema()));
        assertThat(e.getMessage(), is("lastName: expected type: String, found: Null"));
    }

    @Test
    public void rejectsWhenFirstNameIsTooLong() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/tooLongFirstName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        var e = assertThrows(Exception.class, () -> assertThat(envelope, jsonEnvelope().thatMatchesSchema()));
        assertThat(e.getMessage(), is("firstName: expected maxLength: 255, actual: 256"));
    }

    @Test
    public void rejectsWhenLastNameIsTooLong() {
        //given
        final JsonObject payload = readPayloadFromResource("/updateDefendantPayload/tooLongLastName.json");
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.update-defendant-details"),
                payload);

        //then
        var e = assertThrows(Exception.class, () -> assertThat(envelope, jsonEnvelope().thatMatchesSchema()));
        assertThat(e.getMessage(), is("lastName: expected maxLength: 255, actual: 256"));
    }

    private JsonObject readPayloadFromResource(String path) {
        return createReader(UpdateDefendantSchemaTest.class.getResourceAsStream(path))
                .readObject();
    }


}
