package uk.gov.moj.cpp.sjp.domain.transformation.datecreated;

import static java.util.stream.Stream.of;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.moj.cpp.sjp.domain.transformation.datecreated.exception.TransformationException;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.slf4j.Logger;

abstract class BaseEventTransformer implements EventTransformation {

    private static final String SESSION_ID = "sessionId";
    private static final String TRANSFORMATION_DATE_TO_BE_FIXED = "2018-11-26";

    private Enveloper enveloper;

    @Override
    public Action actionFor(JsonEnvelope event) {
        LocalDate createdAt = event.metadata().createdAt()
                .orElseThrow(() -> new TransformationException("createdAt is null"))
                .toLocalDate();

        if (getEventName().equalsIgnoreCase(event.metadata().name())
                && createdAt.toString().equals(TRANSFORMATION_DATE_TO_BE_FIXED)) {
            return TRANSFORM;
        }

        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(JsonEnvelope eventEnvelope) {
        getLogger().info(
                "Transforming event with session Id : {}",
                eventEnvelope.payloadAsJsonObject().getString(SESSION_ID));

        ZonedDateTime createdAt = returnTransformedCreatedAt(eventEnvelope);

        final JsonEnvelope transformedEnvelope = envelopeFrom(
                metadataFrom(eventEnvelope.metadata()).createdAt(createdAt),
                eventEnvelope.payload());

        return of(transformedEnvelope);
    }

    abstract ZonedDateTime returnTransformedCreatedAt(final JsonEnvelope event);

    @Override
    public void setEnveloper(Enveloper enveloper) {
        this.enveloper = enveloper;
    }

    abstract String getEventName();

    abstract Logger getLogger();
}
