package uk.gov.moj.cpp.sjp.transformation;

import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.of;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.stream.Stream;

import javax.json.JsonObject;

@Transformation
public class CaseAssignmentDeletedToCaseUnassignedTransformer implements EventTransformation {

    public static final String CASE_ASSIGNMENT_DELETED_EVENT = "sjp.events.case-assignment-deleted";
    public static final String CASE_UNASSIGNED_EVENT = "sjp.events.case-unassigned";


    @Override
    public Action actionFor(final JsonEnvelope event) {
        return CASE_ASSIGNMENT_DELETED_EVENT.equalsIgnoreCase(event.metadata().name()) ? TRANSFORM : NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope oldEvent) {
        return of(buildNewEnvelope(oldEvent));
    }

    private JsonObject buildNewPayLoad(final JsonEnvelope oldEvent) {
        return createObjectBuilder().add("caseId",  oldEvent.payloadAsJsonObject().getString("caseId")).build();
    }

    private JsonEnvelope buildNewEnvelope(final JsonEnvelope oldEvent) {
        return envelopeFrom(metadataFrom(oldEvent.metadata())
                .withId(randomUUID())
                .withName(CASE_UNASSIGNED_EVENT).build(), buildNewPayLoad(oldEvent));
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        //Not used
    }
}
