package uk.gov.moj.cpp.sjp.event.processor.helper;

import static javax.json.Json.createReader;
import static org.apache.commons.io.Charsets.UTF_8;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonObjectConversionHelper {

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private JsonObjectConversionHelper() {
    }

    @SuppressWarnings("squid:S2095")
    public static byte[] jsonObjectAsByteArray(final JsonObject jsonObject)  {
        return jsonObject.toString().getBytes(UTF_8);
    }

    public static JsonObject streamToJsonObject(final InputStream objectStream) {
        try (final JsonReader jsonReader = createReader(objectStream)) {
            return jsonReader.readObject();
        }
    }

    public static String toJsonString(final Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
