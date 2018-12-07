package uk.gov.moj.cpp.sjp.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.domain.Session;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRequested;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;

public class CaseAssignmentRequestedEventBackwardCompatibilityTest {

    private ObjectMapper objectMapper = new ObjectMapperProducer()
            .objectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

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

        final CaseAssignmentRequested actualCaseAssignmentRequestedEvent = objectMapper.readValue(oldEventPayload.toString(), CaseAssignmentRequested.class);
        final CaseAssignmentRequested expectedCaseAssignmentRequestedEvent = new CaseAssignmentRequested(new Session(sessionId, userId, sessionType, null));

        if (!new ReflectionEquals(actualCaseAssignmentRequestedEvent).matches(expectedCaseAssignmentRequestedEvent)) {
            fail("Old event version can be serialized into event class");
        }

        final String actualCaseAssignmentRequestedEventDeserialized = objectMapper.writeValueAsString(actualCaseAssignmentRequestedEvent);

        // remove json ignored fields
        final String expectedCaseAssignmentRequestedEventDeserialized = createObjectBuilderWithFilter(oldEventPayload, field -> !"session".equals(field))
                .add("session", createObjectBuilderWithFilter(oldEventPayload.getJsonObject("session"), field -> !"localJusticeAreaNationalCourtCode".equals(field)))
                .build()
                .toString();

        assertThat("Old event version can be deserialized into event class",
                actualCaseAssignmentRequestedEventDeserialized,
                Matchers.equalTo(expectedCaseAssignmentRequestedEventDeserialized));
    }

    @Test
    public void shouldHaveEventAnnotation() {
        assertThat(CaseAssignmentRequested.class.isAnnotationPresent(Event.class), is(true));
        assertThat(CaseAssignmentRequested.class.getAnnotation(Event.class).value(), is("sjp.events.case-assignment-requested"));
    }
}
