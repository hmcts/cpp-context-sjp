package uk.gov.moj.cpp.sjp.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.domain.Session;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRequested;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

public class CaseAssignmentRequestedEventBackwardCompatibilityTest {

    @Test
    public void shouldDeserializeOldCaseAssignmentRequestedEvent() throws Exception {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();
        final SessionType sessionType = MAGISTRATE;
        final String ljaNationalCourtCode = "2577";

        final JsonObject oldEventPayload = Json.createObjectBuilder()
                .add("session", Json.createObjectBuilder()
                        .add("id", sessionId.toString())
                        .add("type", MAGISTRATE.toString())
                        .add("userId", userId.toString())
                        .add("localJusticeAreaNationalCourtCode", ljaNationalCourtCode))
                .build();

        final CaseAssignmentRequested actualCaseAssignmentRequestedEvent = new ObjectMapperProducer().objectMapper().readValue(oldEventPayload.toString(), CaseAssignmentRequested.class);
        final CaseAssignmentRequested expectedCaseAssignmentRequestedEvent = new CaseAssignmentRequested(new Session(sessionId, userId, sessionType, null));

        assertThat("Old event version can be serialized into event class", actualCaseAssignmentRequestedEvent, equalTo(expectedCaseAssignmentRequestedEvent));
    }
}
