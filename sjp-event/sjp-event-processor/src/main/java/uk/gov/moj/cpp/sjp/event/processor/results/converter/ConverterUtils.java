package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.UUID.fromString;

import java.util.UUID;

import javax.json.JsonObject;

public class ConverterUtils {

    private ConverterUtils() {
    }

    public static Boolean getBoolean(final JsonObject sjpSessionPayload, final String Key) {
        return sjpSessionPayload.containsKey(Key) && sjpSessionPayload.getBoolean(Key, false);
    }

    public static String getString(final JsonObject sjpSessionPayload, final String key) {
        return sjpSessionPayload.containsKey(key) && !sjpSessionPayload.getString(key).isEmpty() ? sjpSessionPayload.getString(key, null) : null;
    }

    public static UUID extractUUID(final JsonObject object, final String key) {
        return object.containsKey(key) && !object.getString(key).isEmpty() ? fromString(object.getString(key, null)) : null;
    }
}
