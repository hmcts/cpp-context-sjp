package uk.gov.moj.cpp.sjp.event.processor.utils;

import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataFrom;

import uk.gov.justice.services.messaging.Metadata;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class MetadataHelper {

    public static Metadata metadataFromString(final String metadataString) {
        return metadataFrom(fromString(metadataString));
    }

    public static String metadataToString(Metadata metadata) {
        return metadata.asJsonObject().toString();
    }

    private static JsonObject fromString(String json) {
        try (final JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            return jsonReader.readObject();
        }
    }


}
