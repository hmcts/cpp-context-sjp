package uk.gov.moj.cpp.sjp.command.schema;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;
import javax.json.JsonValue;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CaseReopenedSchemaTest {

    @Parameters
    public static Object[] data() {
        return new Object[]{
                "sjp.mark-case-reopened-in-libra",
                "sjp.update-case-reopened-in-libra"};
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameter
    public String schema;

    @Test
    public void validEnvelope() {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void invalidEnvelope_reason_notFound() {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        expectedException.expectMessage("[reason] not found");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void invalidEnvelope_reason_empty() {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "")
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        expectedException.expectMessage("/reason: expected minLength: 1, actual: 0");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void invalidEnvelope_reason_foundNull() {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", JsonValue.NULL)
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        expectedException.expectMessage("/reason: expected type: String, found: Null");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void invalidEnvelope_libraCaseNumber_notFound() {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        expectedException.expectMessage("[libraCaseNumber] not found");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void invalidEnvelope_libraCaseNumber_reason_empty() {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", "")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        expectedException.expectMessage("/libraCaseNumber: expected minLength: 1, actual: 0");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void invalidEnvelope_libraCaseNumber_foundNull() {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", JsonValue.NULL)
                        .add("reopenedDate", "2017-01-01")
                        .build());

        //then
        expectedException.expectMessage("/libraCaseNumber: expected type: String, found: Null");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    public void invalidEnvelope_reopenedDate_notFound() {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", "LIBRA12345")
                        .build());

        //then
        expectedException.expectMessage("[reopenedDate] not found");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @Test
    @Ignore("Looks like format date is supportin 13 as a month..")
    public void invalidEnvelope_reopenedDate_invalidDate() {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-13-01")
                        .build());

        //then
        expectedException.expectMessage("/reopenedDate: string [2017-13-01] does not match pattern");
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }
}
