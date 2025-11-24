package uk.gov.moj.cpp.sjp.command.utils;

import org.apache.commons.lang3.Validate;

import javax.json.JsonObject;
import java.util.Objects;

public class NullSafeJsonObjectHelper {
    private NullSafeJsonObjectHelper() {
    }

    public static boolean notNull(String key, JsonObject o) {
        Validate.notBlank(key);
        if (Objects.isNull(o)) {
            return false;
        }
        return o.containsKey(key) && !o.isNull(key);
    }
}
