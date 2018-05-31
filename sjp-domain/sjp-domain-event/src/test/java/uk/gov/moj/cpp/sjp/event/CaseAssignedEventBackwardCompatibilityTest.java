package uk.gov.moj.cpp.sjp.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

public class CaseAssignedEventBackwardCompatibilityTest {

    @Test
    public void shouldDeserializeOldCaseAssignedEvent() throws Exception {
        final UUID caseId = randomUUID();
        final UUID sessionId = randomUUID();
        final UUID assigneeId = randomUUID();
        final CaseAssignmentType caseAssignmentType = CaseAssignmentType.MAGISTRATE_DECISION;

        final JsonObject oldEventPayload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("assigneeId", assigneeId.toString())
                .add("sessionId", sessionId.toString())
                .add("caseAssignmentType", caseAssignmentType.toString())
                .build();

        final CaseAssigned actualCaseAssignedEvent = new ObjectMapperProducer().objectMapper().readValue(oldEventPayload.toString(), CaseAssigned.class);
        final CaseAssigned expectedCaseAssignedEvent = new CaseAssigned(caseId, assigneeId, null, caseAssignmentType);

        assertThat("Old event version can be serialized into event class", actualCaseAssignedEvent, equalTo(expectedCaseAssignedEvent));
    }
}
