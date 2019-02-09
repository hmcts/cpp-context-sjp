package uk.gov.moj.cpp.sjp.query.view.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.query.view.util.TransparencyServiceUtil.resolveSize;

import org.junit.Test;

public class TransparencyServiceUtilTest {

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


}