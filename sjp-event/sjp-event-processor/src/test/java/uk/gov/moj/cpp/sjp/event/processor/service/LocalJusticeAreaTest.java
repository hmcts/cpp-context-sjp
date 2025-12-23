package uk.gov.moj.cpp.sjp.event.processor.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

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

    @Test
    public void shouldThrowErrorIfArgumentsMissing() {
        final JsonObject localJusticeArea = createObjectBuilder().build();

        assertThrows(NullPointerException.class, () -> LocalJusticeArea.fromJson(localJusticeArea));
    }
}