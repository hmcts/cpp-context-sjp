package uk.gov.moj.cpp.sjp.event.decommissioned;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.event.CourtReferralActioned;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

public class CourtReferralActionedTest {

    @Test
    public void shouldDeserializeDecommissionedCourtReferralActionedEvent() throws Exception {
        final DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
        final JsonObject oldEventPayload = Json.createObjectBuilder()
                .add("caseId", UUID.randomUUID().toString())
                .add("actioned", ZonedDateTime.now().format(dtf))
                .build();

        assertThat("Event class is event annotated",
                CourtReferralActioned.class.getAnnotation(Event.class).value(),
                is("sjp.events.court-referral-actioned"));

        assertThat("Old event can be serialized into event class",
                new ObjectMapperProducer().objectMapper().readValue(oldEventPayload.toString(),
                        CourtReferralActioned.class), notNullValue());
    }
}
