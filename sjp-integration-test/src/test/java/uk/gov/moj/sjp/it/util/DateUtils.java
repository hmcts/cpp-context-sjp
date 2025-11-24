package uk.gov.moj.sjp.it.util;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static final DateTimeFormatter CPP_ZONED_DATE_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
}
