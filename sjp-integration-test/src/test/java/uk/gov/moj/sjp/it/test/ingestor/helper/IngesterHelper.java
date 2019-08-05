package uk.gov.moj.sjp.it.test.ingestor.helper;

import static javax.json.Json.createReader;

import java.io.StringReader;

import javax.json.JsonObject;
import javax.json.JsonReader;

public class IngesterHelper {

    public static JsonObject jsonFromString(final String jsonObjectStr) {
        try (final JsonReader jsonReader = createReader(new StringReader(jsonObjectStr))) {
            return jsonReader.readObject();
        }
    }
}
