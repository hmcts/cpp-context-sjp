package uk.gov.moj.cpp.sjp.command.utils;

import org.junit.jupiter.api.Test;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import javax.json.JsonObject;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NullSafeJsonObjectHelperTest {

    @Test
    public void keyMustNotBeNull() {
        JsonObject jsonObject = createObjectBuilder().build();
        assertThrows(NullPointerException.class, () -> NullSafeJsonObjectHelper.notNull(null, jsonObject));
    }

    @Test
    public void keyMustNotBeEmpty() {
        JsonObject jsonObject = createObjectBuilder().build();
        assertThrows(IllegalArgumentException.class, () -> NullSafeJsonObjectHelper.notNull("", jsonObject));
    }

    @Test
    public void keyMustNotBeBlank() {
        JsonObject jsonObject = createObjectBuilder().build();
        assertThrows(IllegalArgumentException.class, () -> NullSafeJsonObjectHelper.notNull(" ", jsonObject));
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
