package uk.gov.moj.cpp.sjp;

import static org.apache.activemq.artemis.utils.JsonLoader.createReader;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Map;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class EventTransformerTestHelper {

    private BaseEventTransformer eventTransformer;

    public EventTransformerTestHelper(final BaseEventTransformer eventTransformer) {
        this.eventTransformer = eventTransformer;
    }

    public void transformEventAndAssertPayload(final String inputJsonPayloadResourceName,
                                               final String expectedJsonPayloadResourceName) {
        final JsonEnvelope eventEnvelope = buildEnvelopeFromJsonResource(inputJsonPayloadResourceName);
        final JsonEnvelope expectedEnvelope = buildEnvelopeFromJsonResource(expectedJsonPayloadResourceName);
        final JsonObject expectedJsonObject = expectedEnvelope.payloadAsJsonObject();

        final Stream<JsonEnvelope> transformedJsonEnvelopeStream = eventTransformer.apply(eventEnvelope);
        assertPayloadEquals(expectedJsonObject, transformedJsonEnvelopeStream.findFirst().get().payloadAsJsonObject());

        assertEquals(eventTransformer.actionFor(eventEnvelope), TRANSFORM);
        assertEquals(eventTransformer.actionFor(expectedEnvelope), NO_ACTION);
    }


    private JsonEnvelope buildEnvelopeFromJsonResource(final String fileName) {
        final JsonObject payload = createReader(CaseReceivedEventTransformerTest.class
                .getResourceAsStream("/events/" + fileName))
                .readObject();

        return envelopeFrom(metadataWithRandomUUID(eventTransformer.getEventName()), payload);
    }

    private static <K, V> void assertPayloadEquals(Map<K, V> a, Map<K, V> b) {
        assertThat("A doesn't contain B", a.entrySet(), everyItem(isIn(b.entrySet())));
        assertThat("B doesn't contain A", b.entrySet(), everyItem(isIn(a.entrySet())));
    }
}

