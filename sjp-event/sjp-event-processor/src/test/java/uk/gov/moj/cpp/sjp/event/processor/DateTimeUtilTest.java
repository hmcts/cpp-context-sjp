package uk.gov.moj.cpp.sjp.event.processor;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.formatDateTimeForPdfReport;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.formatPublicationDateTimeForJsonReport;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DateTimeUtilTest {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(LocalDateTime.of(2018, 7, 9, 15, 16), false, "Monday, 9 July at 3:16 pm", "2018-07-09T15:16:00.000Z"),
                Arguments.of(LocalDateTime.of(2019, 1, 9, 1, 1), true, "Dydd Mercher, 9 Ionawr am 1:01 yb", "2019-01-09T01:01:00.000Z")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldReturnFormattedDateTime(LocalDateTime dateTime, Boolean isWelsh, String expectedFormattedDateTime, String expectedFormattedPublicationDateTime) {
        assertThat(formatDateTimeForPdfReport(dateTime, isWelsh), is(expectedFormattedDateTime));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldReturnFormattedPublicationDateTime(LocalDateTime dateTime, Boolean isWelsh, String expectedFormattedDateTime, String expectedFormattedPublicationDateTime) {
        assertThat(formatPublicationDateTimeForJsonReport(dateTime, isWelsh), is(expectedFormattedPublicationDateTime));
    }
}