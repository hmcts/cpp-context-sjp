package uk.gov.moj.cpp.sjp.query.view.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.format;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.resolveSize;

import java.time.LocalDateTime;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class TransparencyServiceUtilTest {

    private final LocalDateTime localDateTime = LocalDateTime.of(2018, 12, 25, 0, 1, 35);

    @Test
    public void shouldResolveSizeCorrectlyForFileInMB() {
        final String formattedSize = resolveSize(2663383);
        assertThat(formattedSize, is("2MB"));
    }

    @Test
    public void shouldResolveSizeCorrectlyForFileInKB() {
        final String formattedSize = resolveSize(2600);
        assertThat(formattedSize, is("2KB"));
    }

    @Test
    public void resolveSizeCorrectlyForFileInJustBytes() {
        final String formattedSize = resolveSize(1022);
        assertThat(formattedSize, is("1022B"));
    }

    @Test
    public void shouldFormatTheDateCorrectly() {
        final String formattedSize = format(localDateTime);
        MatcherAssert.assertThat(formattedSize, Matchers.is("25 December 2018 at 12:01am"));
    }

}