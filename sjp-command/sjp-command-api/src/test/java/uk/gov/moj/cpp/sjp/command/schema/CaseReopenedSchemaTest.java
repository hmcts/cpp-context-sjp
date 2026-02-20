package uk.gov.moj.cpp.sjp.command.schema;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import java.util.stream.Stream;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeAll;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;

public class CaseReopenedSchemaTest {

    /**
     * Issue with remote refs, reported to Techpod: https://github.com/CJSCommonPlatform/microservice_framework/issues/648"
     * ============ TODO: delete this block of code once the framework gets updated: ==============
     */
    @BeforeAll
    public static void init() {
        // programmatically ignore all the below test if the framework fails to validate $ref schemas
        assumeThat(haveTechpodFixedReferencesThatMatchesSchema(), is(true));
    }

    private static boolean haveTechpodFixedReferencesThatMatchesSchema() {
        try {
            assertThat(envelopeFrom(metadataWithRandomUUID("sjp.mark-case-reopened-in-libra"), JsonValue.NULL), jsonEnvelope().thatMatchesSchema());
        } catch (Exception e) {
            return !e.getMessage().equals("java.net.UnknownHostException: justice.gov.uk");
        }

        return true;
    }

    // =============================================================================================


    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("sjp.mark-case-reopened-in-libra"),
                Arguments.of("sjp.update-case-reopened-in-libra")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void validEnvelope(String schema) {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        assertThat(envelope, jsonEnvelope().thatMatchesSchema());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void invalidEnvelope_reason_notFound(String schema) {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        JsonEnvelopeMatcher matcher = jsonEnvelope().thatMatchesSchema();
        var e = assertThrows(AssertionError.class, () -> assertThat(envelope, matcher));
        assertThat(e.getMessage(), containsString("[reason] not found"));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void invalidEnvelope_reason_empty(String schema) {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "")
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        JsonEnvelopeMatcher matcher = jsonEnvelope().thatMatchesSchema();
        var e = assertThrows(AssertionError.class, () -> assertThat(envelope, matcher));
        assertThat(e.getMessage(), CoreMatchers.containsString("/reason: expected minLength: 1, actual: 0"));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void invalidEnvelope_reason_foundNull(String schema) {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", JsonValue.NULL)
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        JsonEnvelopeMatcher matcher = jsonEnvelope().thatMatchesSchema();
        var e = assertThrows(AssertionError.class, () -> assertThat(envelope, matcher));
        assertThat(e.getMessage(), CoreMatchers.containsString("/reason: expected type: String, found: Null"));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void invalidEnvelope_libraCaseNumber_notFound(String schema) {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        JsonEnvelopeMatcher matcher = jsonEnvelope().thatMatchesSchema();
        var e = assertThrows(AssertionError.class, () -> assertThat(envelope, matcher));
        assertThat(e.getMessage(), containsString("[libraCaseNumber] not found"));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void invalidEnvelope_libraCaseNumber_reason_empty(String schema) {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", "")
                        .add("reopenedDate", "2017-01-01").build());

        //then
        JsonEnvelopeMatcher matcher = jsonEnvelope().thatMatchesSchema();
        var e = assertThrows(AssertionError.class, () -> assertThat(envelope, matcher));
        assertThat(e.getMessage(), CoreMatchers.containsString("/libraCaseNumber: expected minLength: 1, actual: 0"));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void invalidEnvelope_libraCaseNumber_foundNull(String schema) {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", JsonValue.NULL)
                        .add("reopenedDate", "2017-01-01")
                        .build());

        //then
        JsonEnvelopeMatcher matcher = jsonEnvelope().thatMatchesSchema();
        var e = assertThrows(AssertionError.class, () -> assertThat(envelope, matcher));
        assertThat(e.getMessage(), CoreMatchers.containsString("/libraCaseNumber: expected type: String, found: Null"));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void invalidEnvelope_reopenedDate_notFound(String schema) {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", "LIBRA12345")
                        .build());

        //then
        JsonEnvelopeMatcher matcher = jsonEnvelope().thatMatchesSchema();
        var e = assertThrows(AssertionError.class, () -> assertThat(envelope, matcher));
        assertThat(e.getMessage(), CoreMatchers.containsString("[reopenedDate] not found"));
    }

    @ParameterizedTest
    @MethodSource("data")
    @Disabled("Looks like format date is supportin 13 as a month..")
    public void invalidEnvelope_reopenedDate_invalidDate(String schema) {
        //given
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(schema),
                Json.createObjectBuilder()
                        .add("reason", "Reason")
                        .add("libraCaseNumber", "LIBRA12345")
                        .add("reopenedDate", "2017-13-01")
                        .build());

        //then
        JsonEnvelopeMatcher matcher = jsonEnvelope().thatMatchesSchema();
        var e = assertThrows(AssertionError.class, () -> assertThat(envelope, matcher));
        assertThat(e.getMessage(), CoreMatchers.containsString("/reopenedDate: string [2017-13-01] does not match pattern"));
    }
}
