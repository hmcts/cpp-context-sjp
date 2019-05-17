package uk.gov.moj.cpp.sjp.event.processor;


import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.formatDateTimeForReport;

import java.time.LocalDateTime;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DateTimeUtilTest {

    @Parameterized.Parameter
    public LocalDateTime dateTime;

    @Parameterized.Parameter(1)
    public Boolean isWelsh;

    @Parameterized.Parameter(2)
    public String expectedFormattedDateTime;

    @Parameterized.Parameters(name = "{0}, welsh={1} formatted to {2}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {LocalDateTime.of(2018, 7, 9, 15, 16), false, "Monday, 9 July at 3:16 pm"},
                {LocalDateTime.of(2019, 1, 9, 1, 1), true, "Dydd Mercher, 9 Ionawr am 1:01 am"}
        });
    }

    @Test
    public void shouldReturnFormattedDateTime() {
        assertThat(formatDateTimeForReport(dateTime, isWelsh), is(expectedFormattedDateTime));
    }
}