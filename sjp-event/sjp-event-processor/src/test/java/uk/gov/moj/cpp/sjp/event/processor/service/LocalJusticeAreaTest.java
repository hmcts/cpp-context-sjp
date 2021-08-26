package uk.gov.moj.cpp.sjp.event.processor.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import javax.json.JsonObject;

import org.junit.Test;

public class LocalJusticeAreaTest {

    @Test
    public void shouldCreateFromJson() {
        final JsonObject localJusticeArea = createObjectBuilder()
                .add("nationalCourtCode", "2577")
                .add("name", "South West London Magistrates' Court")
                .build();

        final LocalJusticeArea actual = LocalJusticeArea.fromJson(localJusticeArea);

        assertThat(actual.getName(), equalTo("South West London Magistrates' Court"));
        assertThat(actual.getNationalCourtCode(), equalTo("2577"));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowErrorIfArgumentsMissing() {
        final JsonObject localJusticeArea = createObjectBuilder().build();

        LocalJusticeArea.fromJson(localJusticeArea);
    }
}