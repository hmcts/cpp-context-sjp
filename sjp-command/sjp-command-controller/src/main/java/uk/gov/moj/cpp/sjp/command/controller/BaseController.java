package uk.gov.moj.cpp.sjp.command.controller;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public abstract class BaseController {

    JsonObject addJsonField(final JsonObject jsonObj, final String key, final String value) {
        if (nonNull(jsonObj)) {
            return createObjectBuilder(jsonObj)
                    .add(key, value).build();
        }
        return jsonObj;
    }

    JsonObject updateJsonObject(final JsonObject jsonObj, final String key, final JsonValue newJsonValue) {
        if (jsonObj.containsKey(key)) {
            final JsonObjectBuilder newObjectBuilder = createObjectBuilderWithFilter(jsonObj, field -> !key.equals(field));
            newObjectBuilder.add(key, newJsonValue);
            return newObjectBuilder.build();
        }
        return jsonObj;
    }
}
