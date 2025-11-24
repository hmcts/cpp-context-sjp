package uk.gov.moj.cpp.sjp.event.processor.utils;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonHelper {

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private JsonHelper() {
    }

    public static <T> T fromJsonString(final String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static JsonObject toJsonObject(final Object payload) {
        try {
            final String json = objectMapper.writeValueAsString(payload);
            return fromJsonString(json, JsonObject.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
