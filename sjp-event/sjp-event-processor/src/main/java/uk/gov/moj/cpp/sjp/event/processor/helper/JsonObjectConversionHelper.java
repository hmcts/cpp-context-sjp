package uk.gov.moj.cpp.sjp.event.processor.helper;

import static javax.json.Json.createReader;
import static org.apache.commons.io.Charsets.UTF_8;

import java.io.InputStream;

import javax.json.JsonObject;
import javax.json.JsonReader;

public class JsonObjectConversionHelper {

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

}
