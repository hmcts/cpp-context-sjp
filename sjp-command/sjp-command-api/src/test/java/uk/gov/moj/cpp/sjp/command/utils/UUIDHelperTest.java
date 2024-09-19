package uk.gov.moj.cpp.sjp.command.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class UUIDHelperTest {

    @Test
    public void shouldReturnTrueForValidUUIDs() {
        assertThat(UUIDHelper.isUuid(UUID.randomUUID().toString()), is(true));
        assertThat(UUIDHelper.isUuid(UUID.randomUUID().toString()), is(true));
        assertThat(UUIDHelper.isUuid(UUID.randomUUID().toString()), is(true));
    }

    @Test
    public void shouldReturnFalseWhenValueIsNull() {
        assertThat(UUIDHelper.isUuid(null), is(false));
    }

    @Test
    public void shouldReturnFalseWhenUuidIsInWrongFormat() {
        assertThat(UUIDHelper.isUuid(""), is(false));
        assertThat(UUIDHelper.isUuid(" "), is(false));
        assertThat(UUIDHelper.isUuid("null"), is(false));
        assertThat(UUIDHelper.isUuid("0000000-0000-0000-0000-000000000000"), is(false)); // missing one char
    }
}