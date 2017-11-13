package uk.gov.moj.cpp.sjp.event.processor.utils;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
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

    public Metadata metadataFromString(final String metadataString) {
        return metadataFrom(readJson(metadataString));
    }

    public String metadataToString(final Metadata metadata) {
        return metadata.asJsonObject().toString();
    }

    @SuppressWarnings("deprecation")
    public JsonEnvelope envelopeWithSjpProcessId(final Metadata originalMetadata, final JsonObject payload, final String processId) {
        final Metadata newMetadata = metadataWithSjpProcessId(originalMetadata, processId);
        final JsonObject payloadWithMetadata = payloadWithMetadata(payload, newMetadata);
        return envelopeFrom(newMetadata, payloadWithMetadata);
    }

    public Optional<String> getSjpProcessId(final JsonEnvelope envelope) {
        return ofNullable(envelope.metadata().asJsonObject().getString(SJP_ID, null));
    }


    private Metadata metadataWithSjpProcessId(final Metadata metadata, final String processId) {
        final JsonObjectBuilder newMetadataJson = createObjectBuilder(metadata.asJsonObject());
        newMetadataJson.add(SJP_ID, processId);
        return metadataFrom(newMetadataJson.build());
    }

    private JsonObject payloadWithMetadata(final JsonObject payload, final Metadata metadata) {
        final Function<String, Boolean> excludeCausation = key -> !JsonObjectMetadata.CAUSATION.equals(key);
        final JsonObjectBuilder updatedMetadataBuilder = JsonObjects.createObjectBuilderWithFilter(metadata.asJsonObject(), excludeCausation);
        final JsonObjectBuilder updatedPayloadBuilder = createObjectBuilder(payload);
        updatedPayloadBuilder.add(JsonEnvelope.METADATA, updatedMetadataBuilder);
        return updatedPayloadBuilder.build();
    }

    private JsonObject readJson(final String json) {
        try (final JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            return jsonReader.readObject();
        }
    }

}
