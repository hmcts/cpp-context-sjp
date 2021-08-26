package uk.gov.moj.cpp.sjp.event.processor.utils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class NumberUtilsTest {

    @Test
    public void greaterThanZero() {
        assertThat(NumberUtils.greaterThanZero(0), is(false));
        assertThat(NumberUtils.greaterThanZero(-1), is(false));
        assertThat(NumberUtils.greaterThanZero(null), is(false));
        assertThat(NumberUtils.greaterThanZero(1), is(true));
    }
}