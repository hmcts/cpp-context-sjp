package uk.gov.moj.sjp.it.test.ingestor.helper;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createReader;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.io.StringReader;
import java.util.Random;

import javax.json.JsonObject;
import javax.json.JsonReader;

public class IngesterHelper {

    public static JsonObject jsonFromString(final String jsonObjectStr) {
        try (final JsonReader jsonReader = createReader(new StringReader(jsonObjectStr))) {
            return jsonReader.readObject();
        }
    }

    public static JsonEnvelope buildEnvelope(final String payload, final String eventName) {
        final int generatedInteger = new Random().nextInt();
        final JsonReader jsonReader = createReader(new StringReader(payload));
        final MetadataBuilder metadata = metadataBuilder().withStreamId(randomUUID()).withId(randomUUID())
                .withName(eventName)
                .withVersion(1)
                .withPreviousEventNumber(generatedInteger)
                .withEventNumber(generatedInteger+1)
                .withSource("people.event.source");
        final JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();
        return envelopeFrom(metadata.build(), jsonObject);
    }
}
