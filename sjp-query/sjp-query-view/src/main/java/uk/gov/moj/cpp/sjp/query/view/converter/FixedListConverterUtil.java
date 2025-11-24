package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Optional;

import javax.json.JsonObject;

public class FixedListConverterUtil {

    private FixedListConverterUtil() { }

    public static Optional<String> mapValue(final String value, final Map<String, String> valueMap) {
        return ofNullable(valueMap.get(value.trim().toLowerCase()));
    }

    public static Optional<JsonObject> mapToReferenceDataFixedList(final Optional<JsonObject> allFixedListResult, final String id) {
        return allFixedListResult.map(afr -> afr.getJsonArray("fixedListCollection").getValuesAs(JsonObject.class).stream().filter(fixedListItem -> id.equals(fixedListItem.getString("id"))).findFirst()).map(Optional::get);
    }
}
