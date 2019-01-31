package uk.gov.moj.cpp.sjp.domain.transformation.defendant;


import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;

@Transformation
public class DefendantUpdatedDateTransformer implements EventTransformation {

    private static final Logger LOGGER = getLogger(DefendantUpdatedDateTransformer.class);
    private static final String DEFENDANT_UPDATED_EVENT_NAME = "sjp.events.defendant-details-updated";
    private static final String UPDATED_DATE_KEY = "updatedDate";
    private static final String CASE_ID_KEY = "caseId";

    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        if (DEFENDANT_UPDATED_EVENT_NAME.equalsIgnoreCase(eventEnvelope.metadata().name())
                && !eventEnvelope.payloadAsJsonObject().containsKey(UPDATED_DATE_KEY)) {
            return TRANSFORM;
        }
        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope envelope) {
        LOGGER.info(
                "Transforming event with Case Id : {}",
                envelope.payloadAsJsonObject().getString(CASE_ID_KEY));

        final JsonValue transformedEvent = buildTransformedEventPayload(envelope);

        final JsonEnvelope transformedEnvelope = envelopeFrom(
                envelope.metadata(),
                transformedEvent);

        return of(transformedEnvelope);
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {

    }

    private JsonValue buildTransformedEventPayload(final JsonEnvelope event) {
        final JsonObject eventPayload = event.payloadAsJsonObject();

        return JsonObjects.createObjectBuilder(eventPayload)
                .add(UPDATED_DATE_KEY, ZonedDateTimes.toString(
                        event.metadata().createdAt().orElseGet(ZonedDateTime::now))).build();
    }
}
