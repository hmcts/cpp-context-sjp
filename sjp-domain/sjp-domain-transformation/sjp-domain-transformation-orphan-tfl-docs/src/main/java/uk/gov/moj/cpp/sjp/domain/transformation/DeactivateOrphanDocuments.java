package uk.gov.moj.cpp.sjp.domain.transformation;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.stream.Stream;

import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

@Transformation
public class DeactivateOrphanDocuments implements EventTransformation {

    private static final String EVENT_CASE_DOCUMENT_ADDED = "sjp.events.case-document-added";
    private static final long EVENT_FIRST_IN_STREAM_POSITION = 1l;

    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        final Metadata metadata = eventEnvelope.metadata();
        if (metadata.name().equals(EVENT_CASE_DOCUMENT_ADDED) &&
                metadata.position().orElse(0l).equals(EVENT_FIRST_IN_STREAM_POSITION)) {
            return DEACTIVATE;
        }
        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope envelope) {
        return Stream.empty();
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // No action to take here
    }
}
