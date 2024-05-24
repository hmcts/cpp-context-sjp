package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.ENGLISH;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateTimeUtil {

    public static final String DATE_PATTERN = "EEEE, d MMMM";
    public static final String TIME_PATTERN = "h:mm a";
    public static final String PUBLICATION_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String TITLE_DATE_PATTERN = "d MMMM";
    public static final DateTimeFormatter ENGLISH_DATE_FORMATTER = ofPattern(DATE_PATTERN).withLocale(new Locale("en"));
    public static final DateTimeFormatter WELSH_DATE_FORMATTER = ENGLISH_DATE_FORMATTER.withLocale(new Locale("cy"));
    public static final DateTimeFormatter ENGLISH_TIME_FORMATTER = ofPattern(TIME_PATTERN).withLocale(new Locale("en"));
    public static final DateTimeFormatter WELSH_TIME_FORMATTER = ENGLISH_TIME_FORMATTER.withLocale(new Locale("cy"));
    public static final DateTimeFormatter PUBLICATION_DATE_FORMATTER = ofPattern(PUBLICATION_DATE_PATTERN);
    public static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MM yyyy");
    public static final DateTimeFormatter DOB_FORMAT = ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter START_DATE_FORMAT = ofPattern("dd MMMM yyyy");
    public static final DateTimeFormatter ENGLISH_TITLE_DATE_FORMATTER = ofPattern(TITLE_DATE_PATTERN).withLocale(new Locale("en"));

    private DateTimeUtil() {
        // static utility class, hiding the public constructor
    }

    public static String formatDateTimeForPdfReport(final LocalDateTime dateTime, final Boolean isWelsh) {
        return dateTime.format(isWelsh ? WELSH_DATE_FORMATTER : ENGLISH_DATE_FORMATTER)
                .concat(isWelsh ? " am " : " at ")
                .concat(dateTime.format(isWelsh ? WELSH_TIME_FORMATTER : ENGLISH_TIME_FORMATTER))
                .replace(" AM", " am")
                .replace(" PM", " pm");
    }

    public static String formatPublicationDateTimeForJsonReport(final LocalDateTime dateTime, final Boolean isWelsh) {
        return dateTime.format(PUBLICATION_DATE_FORMATTER.withLocale(isWelsh ? new Locale("cy") : ENGLISH));
    }

}
