package uk.gov.moj.cpp.sjp.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.domain.Session;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRequested;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
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

        final JsonObject oldEventPayload = JsonObjects.createObjectBuilder()
                .add("session", JsonObjects.createObjectBuilder()
                        .add("id", sessionId.toString())
                        .add("localJusticeAreaNationalCourtCode", ljaNationalCourtCode)
                        .add("type", MAGISTRATE.toString())
                        .add("userId", userId.toString()))
                .build();

        final CaseAssignmentRequested actualCaseAssignmentRequestedEvent = objectMapper.readValue(oldEventPayload.toString(), CaseAssignmentRequested.class);
        final CaseAssignmentRequested expectedCaseAssignmentRequestedEvent = new CaseAssignmentRequested(new Session(sessionId, userId, sessionType, null, ljaNationalCourtCode, null));

        if (!new ReflectionEquals(actualCaseAssignmentRequestedEvent).matches(expectedCaseAssignmentRequestedEvent)) {
            fail("Old event version can be serialized into event class");
        }

        final String actualCaseAssignmentRequestedEventDeserialized = objectMapper.writeValueAsString(actualCaseAssignmentRequestedEvent);

        // remove json ignored fields
        final String expectedCaseAssignmentRequestedEventDeserialized = createObjectBuilderWithFilter(oldEventPayload, field -> !"session".equals(field))
                .add("session", createObjectBuilder(oldEventPayload.getJsonObject("session")))
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
