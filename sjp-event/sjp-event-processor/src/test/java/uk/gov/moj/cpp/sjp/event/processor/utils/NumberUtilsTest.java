package uk.gov.moj.cpp.sjp.event.processor.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class NumberUtilsTest {

    @Test
    public void greaterThanZero() {
        assertThat(NumberUtils.greaterThanZero(0), is(false));
        assertThat(NumberUtils.greaterThanZero(-1), is(false));
        assertThat(NumberUtils.greaterThanZero(null), is(false));
        assertThat(NumberUtils.greaterThanZero(1), is(true));
    }
}