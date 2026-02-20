package uk.gov.moj.sjp.it.util;

import static java.util.Objects.isNull;
import static org.skyscreamer.jsonassert.JSONCompare.compareJSON;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonHelper.class);

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

    public static boolean lenientCompare(final JsonObject json1, final JsonObject json2) {
        if (isNull(json1) || isNull(json2)) {
            return false;
        }
        final JSONCompareResult compareResult = compareJSON(json1.toString(), json2.toString(), LENIENT);
        if (compareResult.passed()) {
            return true;
        } else {
            LOGGER.error(compareResult.getMessage());
            return false;
        }
    }

    public static boolean strictCompareIgnoringSelectedData(final JsonObject json1, final JsonObject json2, List<String> ignoreComparisonList) {
        if (isNull(json1) || isNull(json2)) {
            return false;
        }
        JSONCompareResult compareResult = null;
        if (CollectionUtils.isNotEmpty(ignoreComparisonList)) {
            compareResult = compareJSON(json1.toString(), json2.toString(),
                    new CustomComparator(STRICT, ignoreComparisonList.stream()
                            .map(ignore -> new Customization(ignore, (o1, o2) -> true)).toArray(Customization[]::new)));
        } else {
            compareResult = compareJSON(json1.toString(), json2.toString(), STRICT);
        }
        if (compareResult.passed()) {
            return true;
        } else {
            LOGGER.error(compareResult.getMessage());
            return false;
        }
    }

    public static boolean lenientCompare(JsonObject json1, JSONObject json2) {
        return lenientCompare(json1, JsonHelper.getJsonObject(json2.toString()));
    }
}
