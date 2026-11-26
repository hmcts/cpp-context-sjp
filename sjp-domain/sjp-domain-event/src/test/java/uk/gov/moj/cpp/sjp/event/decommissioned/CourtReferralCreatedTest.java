package uk.gov.moj.cpp.sjp.event.decommissioned;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.event.CourtReferralCreated;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class CourtReferralCreatedTest {

    @Test
    public void shouldDeserializeDecommissionedCourtReferralCreatedEvent() throws Exception {
        final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE;
        final JsonObject oldEventPayload = createObjectBuilder()
                .add("caseId", UUID.randomUUID().toString())
                .add("hearingDate", LocalDate.now().format(dtf))
                .build();

        assertThat("Event class is event annotated",
                CourtReferralCreated.class.getAnnotation(Event.class).value(),
                is("sjp.events.court-referral-created"));

        assertThat("Old event can be serialized into event class",
                new ObjectMapperProducer().objectMapper().readValue(oldEventPayload.toString(),
                        CourtReferralCreated.class), notNullValue());
    }
}
