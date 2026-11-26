package uk.gov.moj.cpp.sjp.event.decommissioned;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class CaseAssignmentDeletedTest {

    @Test
    public void shouldDeserializeDecommissionedCaseAssignmentDeletedEvent() throws Exception {
        final JsonObject oldEventPayload = createObjectBuilder()
                .add("caseId", UUID.randomUUID().toString())
                .add("caseAssignmentType", CaseAssignmentType.MAGISTRATE_DECISION.toString())
                .build();

        assertThat("Event class is event annotated",
                CaseAssignmentDeleted.class.getAnnotation(Event.class).value(),
                is("sjp.events.case-assignment-deleted"));

        assertThat("Old event can be serialized into event class",
                new ObjectMapperProducer().objectMapper().readValue(oldEventPayload.toString(),
                        CaseAssignmentDeleted.class), notNullValue());
    }
}
