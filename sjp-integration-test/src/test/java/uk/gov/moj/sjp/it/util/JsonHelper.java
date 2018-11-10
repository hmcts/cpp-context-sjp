package uk.gov.moj.sjp.it.util;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class JsonHelper {

    public static JsonObject getJsonObject(final String json) {
        try (final JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }

    public static <T> T fromJsonString(final String json, Class<T> type) {
        try {
            return new ObjectMapperProducer().objectMapper().readValue(json, type);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T fromJsonObject(final JsonObject json, Class<T> type) {
        return fromJsonString(json.toString(), type);
    }

    public static <T> JsonObject toJsonObject(final T object) {
        try (final StringWriter stringWriter = new StringWriter()) {
            new ObjectMapperProducer().objectMapper().writeValue(stringWriter, object);
            return getJsonObject(stringWriter.toString());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
