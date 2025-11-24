package uk.gov.moj.cpp.sjp.event.processor.utils;

import java.util.Optional;

public class NumberUtils {

    private NumberUtils() {
    }

    public static boolean greaterThanZero(final Integer number) {
        return Optional.ofNullable(number).orElse(0) > 0;
    }
}