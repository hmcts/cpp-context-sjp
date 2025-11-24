package uk.gov.moj.cpp.sjp.query.view.util;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransparencyServiceUtil {

    private static final Integer UNIT_SIZE = 1024;
    private static final String DATE_TIME_PATTERN = "d MMMM YYYY 'at' h:mma";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = ofPattern(DATE_TIME_PATTERN);


    private TransparencyServiceUtil() {
    }

    public static String resolveSize(final Integer sizeInBytes) {
        String formattedValue;

        // we cant a have file in GB and over and dont need the floating values.
        if (sizeInBytes / (UNIT_SIZE * UNIT_SIZE) >= 1) {
            formattedValue = (sizeInBytes / (UNIT_SIZE * UNIT_SIZE)) + "MB";
        } else if (sizeInBytes /  UNIT_SIZE >= 1) {
            formattedValue = (sizeInBytes / UNIT_SIZE) + "KB";
        } else {
            formattedValue = sizeInBytes.toString() + "B";
        }

        return formattedValue;
    }

    public static String format(final LocalDateTime localDateTime) {
        return localDateTime
                .format(DATE_TIME_FORMATTER)
                .replace("AM", "am")
                .replace("PM", "pm");
    }

}
