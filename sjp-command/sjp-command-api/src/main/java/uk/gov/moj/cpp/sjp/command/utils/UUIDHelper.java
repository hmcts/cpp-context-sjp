package uk.gov.moj.cpp.sjp.command.utils;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class UUIDHelper {

    /**
     * This is the same pattern used in the core-domain regex for schema validation
     */
    private static final String UUID_REGEX_PATTERN = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$";

    private UUIDHelper() {
    }

    public static boolean isUuid(final String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return Pattern.matches(UUID_REGEX_PATTERN, value);
    }
}
