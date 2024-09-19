package uk.gov.moj.cpp.sjp.event.decommissioned;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.CourtReferralNotFound;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class CourtReferralNotFoundTest {

    @Test
    public void shouldDeserializeDecommissionedCourtReferralNotFoundEvent() throws Exception {
        final JsonObject oldEventPayload = Json.createObjectBuilder()
                .add("caseId", UUID.randomUUID().toString())
                .build();

        assertThat("Event class is event annotated",
                CourtReferralNotFound.class.getAnnotation(Event.class).value(),
                is("sjp.events.court-referral-not-found"));

        assertThat("Old event can be serialized into event class",
                new ObjectMapperProducer().objectMapper().readValue(oldEventPayload.toString(),
                        CourtReferralNotFound.class), notNullValue());
    }
}
