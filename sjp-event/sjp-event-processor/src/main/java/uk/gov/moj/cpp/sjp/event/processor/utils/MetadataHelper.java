package uk.gov.moj.cpp.sjp.event.processor.utils;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;

import java.io.StringReader;
import java.util.Optional;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

public class MetadataHelper {

    private static final String SJP_ID = "sjpId";

    public static Metadata metadataFromString(final String metadataString) {
        return metadataFrom(readJson(metadataString)).build();
    }

    public static String metadataToString(final Metadata metadata) {
        return metadata.asJsonObject().toString();
    }

    public JsonEnvelope envelopeWithSjpProcessId(final Metadata originalMetadata, final JsonObject payload, final String processId) {
        final Metadata newMetadata = metadataWithSjpProcessId(originalMetadata, processId);
        final JsonObject payloadWithMetadata = payloadWithMetadata(payload, newMetadata);

        return envelopeFrom(newMetadata, payloadWithMetadata);
    }

    public Optional<String> getSjpProcessId(final JsonEnvelope envelope) {
        return ofNullable(envelope.metadata().asJsonObject().getString(SJP_ID, null));
    }

    private Metadata metadataWithSjpProcessId(final Metadata metadata, final String processId) {
        return metadataFrom(
                createObjectBuilder(metadata.asJsonObject())
                        .add(SJP_ID, processId)
                        .build())
                .build();
    }

    private JsonObject payloadWithMetadata(final JsonObject payload, final Metadata metadata) {
        final Function<String, Boolean> excludeCausation = key -> !JsonMetadata.CAUSATION.equals(key);
        final JsonObjectBuilder updatedMetadataBuilder = JsonObjects.createObjectBuilderWithFilter(metadata.asJsonObject(), excludeCausation);

        return createObjectBuilder(payload)
                .add(JsonEnvelope.METADATA, updatedMetadataBuilder)
                .build();
    }

    private static JsonObject readJson(final String json) {
        try (final JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            return jsonReader.readObject();
        }
    }

}
