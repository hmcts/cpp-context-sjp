package uk.gov.moj.cpp.sjp.domain.transformation.offencedate;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OffenceDateTransformerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private OffenceDateTransformer transformer;

    @Test
    public void shouldNotProcessEventOtherThanCaseReceived() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.sjp-case-created"),
                JsonValue.NULL);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldNotProcessCaseReceivedEventWithoutOffenceDate() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-received"),
                readJson("case-received-transformed.json", JsonValue.class));
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldProcessCaseReceivedEventWithOffenceDate() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-received"),
                readJson("case-received.json", JsonValue.class));
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(TRANSFORM));

        verifyTransformation(envelope, "case-received-transformed.json");
    }

    private Action whenTransformerActionIsCheckedFor(final JsonEnvelope envelope) {
        return transformer.actionFor(envelope);
    }

    private void verifyTransformation(final JsonEnvelope envelope, final String expectedFileName) {
        final Stream<JsonEnvelope> jsonEnvelopeStream = transformer.apply(envelope);
        final List<JsonEnvelope> actual = jsonEnvelopeStream.collect(toList());
        assertThat(actual, hasSize(1));

        final JsonObject expectedPayload = envelopeFrom(
                metadataWithRandomUUID(envelope.metadata().name()),
                readJson(expectedFileName, JsonValue.class))
                .payloadAsJsonObject();

        assertThat(actual.get(0).payloadAsJsonObject(), is(expectedPayload));
    }

    private static <T> T readJson(final String jsonPath, final Class<T> clazz, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(jsonPath)) {
            return OBJECT_MAPPER.readValue(format(IOUtils.toString(systemResourceAsStream), placeholders), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible ", e);
        }
    }
}