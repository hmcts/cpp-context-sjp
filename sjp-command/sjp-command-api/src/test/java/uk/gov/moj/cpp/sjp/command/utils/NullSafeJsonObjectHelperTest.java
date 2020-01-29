package uk.gov.moj.cpp.sjp.command.utils;

import org.junit.Test;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class NullSafeJsonObjectHelperTest {

    @Test(expected = NullPointerException.class)
    public void keyMustNotBeNull() {
        NullSafeJsonObjectHelper.notNull(null, createObjectBuilder().build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyMustNotBeEmpty() {
        NullSafeJsonObjectHelper.notNull("", createObjectBuilder().build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyMustNotBeBlank() {
        NullSafeJsonObjectHelper.notNull(" ", createObjectBuilder().build());
    }

    @Test
    public void nullJsonObjectReturnsFalse() {
        assertThat(NullSafeJsonObjectHelper.notNull("myKey", null), is(false));
    }

    @Test
    public void nullValueForKeyReturnsFalse() {
        assertThat(NullSafeJsonObjectHelper.notNull("myKey", createObjectBuilder().addNull("kyKey").build()), is(false));
    }

    @Test
    public void valueNotPresentForKeyReturnsFalse() {
        assertThat(NullSafeJsonObjectHelper.notNull("myKey", createObjectBuilder().build()), is(false));
    }

    @Test
    public void valuePresentReturnTrue() {
        assertThat(NullSafeJsonObjectHelper.notNull("myKey", createObjectBuilder().add("myKey", "some value").build()), is(true));
    }
}
