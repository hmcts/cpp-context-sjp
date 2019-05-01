package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateTimeUtil {

    private static final String DATE_PATTERN = "EEEE, d MMMM";
    private static final String TIME_PATTERN = "h:mm a";
    private static final DateTimeFormatter ENGLISH_DATE_FORMATTER = ofPattern(DATE_PATTERN).withLocale(new Locale("en"));
    private static final DateTimeFormatter WELSH_DATE_FORMATTER = ENGLISH_DATE_FORMATTER.withLocale(new Locale("cy"));
    private static final DateTimeFormatter ENGLISH_TIME_FORMATTER = ofPattern(TIME_PATTERN).withLocale(new Locale("en"));
    private static final DateTimeFormatter WELSH_TIME_FORMATTER = ENGLISH_TIME_FORMATTER.withLocale(new Locale("cy"));

    private DateTimeUtil() {
        // static utility class, hiding the public constructor
    }

    public static String formatDateTimeForReport(final LocalDateTime dateTime, final Boolean isWelsh) {
        return dateTime.format(isWelsh ? WELSH_DATE_FORMATTER : ENGLISH_DATE_FORMATTER)
                .concat(isWelsh ? " am " : " at ")
                .concat(dateTime.format(isWelsh ? WELSH_TIME_FORMATTER : ENGLISH_TIME_FORMATTER))
                .replace(" AM", " am")
                .replace(" PM", " pm");
    }

}
