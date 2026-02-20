package uk.gov.moj.cpp.sjp.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;

public class CaseAssignedEventBackwardCompatibilityTest {

    private ObjectMapper objectMapper = new ObjectMapperProducer()
            .objectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

    @Test
    public void shouldDeserializeOldCaseAssignedEvent() throws Exception {
        final UUID caseId = randomUUID();
        final UUID sessionId = randomUUID();
        final UUID assigneeId = randomUUID();
        final CaseAssignmentType caseAssignmentType = CaseAssignmentType.MAGISTRATE_DECISION;

        final JsonObject oldEventPayload = createObjectBuilder()
                .add("assigneeId", assigneeId.toString())
                .add("caseAssignmentType", caseAssignmentType.toString())
                .add("caseId", caseId.toString())
                .add("sessionId", sessionId.toString())
                .build();


        final String inputEvent = oldEventPayload.toString();
        final CaseAssigned actualCaseAssignedEvent = objectMapper.readValue(inputEvent, CaseAssigned.class);
        final CaseAssigned expectedCaseAssignedEvent = new CaseAssigned(caseId, assigneeId, null, caseAssignmentType);

        if (!new ReflectionEquals(actualCaseAssignedEvent).matches(expectedCaseAssignedEvent)) {
            fail("Old event version can be serialized into event class");
        }

        final String actualCaseAssignedEventDeserialized = objectMapper.writeValueAsString(actualCaseAssignedEvent);

        // remove json ignored fields
        final String expectedCaseAssignedEventDeserialized = createObjectBuilderWithFilter(oldEventPayload, field -> !"sessionId".equals(field)).build().toString();

        assertThat("Old event version can be deserialized into event class",
                actualCaseAssignedEventDeserialized,
                equalTo(expectedCaseAssignedEventDeserialized));
    }

    @Test
    public void shouldHaveEventAnnotation() {
        assertThat(CaseAssigned.class.isAnnotationPresent(Event.class), is(true));
        assertThat(CaseAssigned.class.getAnnotation(Event.class).value(), is("sjp.events.case-assigned"));
    }

}
