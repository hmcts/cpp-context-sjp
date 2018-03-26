package uk.gov.moj.cpp.sjp.event.decommissioned;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

public class CaseAssignmentCreatedTest {

    @Test
    public void shouldDeserializeDecommissionedCaseAssignmentCreatedEvent() throws Exception {
        final JsonObject oldEventPayload = Json.createObjectBuilder()
                .add("caseId", UUID.randomUUID().toString())
                .add("caseAssignmentType", CaseAssignmentType.MAGISTRATE_DECISION.toString())
                .build();

        assertThat("Event class is event annotated",
                CaseAssignmentCreated.class.getAnnotation(Event.class).value(),
                is("sjp.events.case-assignment-created"));

        assertThat("Old event can be serialized into event class",
                new ObjectMapperProducer().objectMapper().readValue(oldEventPayload.toString(),
                        CaseAssignmentCreated.class), notNullValue());
    }
}
