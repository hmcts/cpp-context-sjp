package uk.gov.moj.cpp.sjp.transformation;

import static java.util.stream.Stream.of;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.sjp.transformation.util.SchemaValidatorUtil.validateAgainstSchema;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.moj.cpp.sjp.transformation.data.CourtHouseDataSource;

import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

abstract class BaseCourtCodeEnrichmentTransformer implements EventTransformation {

    private static final String SESSION_ID = "sessionId";
    private static final String COURT_HOUSE_NAME_KEY = "courtHouseName";
    private static final String COURT_HOUSE_CODE_KEY = "courtHouseCode";

    private final CourtHouseDataSource courtHouseDataSource;
    private Enveloper enveloper;

    @VisibleForTesting
    BaseCourtCodeEnrichmentTransformer(final CourtHouseDataSource courtHouseDataSource) {
        this.courtHouseDataSource = courtHouseDataSource;
    }

    @Override
    public Action actionFor(JsonEnvelope event) {
        if (getEventName().equalsIgnoreCase(event.metadata().name())) {
            return TRANSFORM;
        }

        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(JsonEnvelope eventEnvelope) {
        if (eventEnvelope.payloadAsJsonObject().containsKey(COURT_HOUSE_CODE_KEY)) {
            return of(eventEnvelope);
        }

        getLogger().info(
                "Transforming event with session Id : {}",
                eventEnvelope.payloadAsJsonObject().getString(SESSION_ID));

        final JsonValue transformedEvent = buildTransformedEventPayload(eventEnvelope);
        validateAgainstSchema(getEventSchemaFileName(), transformedEvent.toString());

        final JsonEnvelope transformedEnvelope = envelopeFrom(
                eventEnvelope.metadata(),
                transformedEvent);


        return of(transformedEnvelope);
    }

    private JsonValue buildTransformedEventPayload(final JsonEnvelope event) {
        JsonObject eventPayload = event.payloadAsJsonObject();
        String courtCodeForName = courtHouseDataSource.getCourtCodeForName(eventPayload.getString(COURT_HOUSE_NAME_KEY));

        return JsonObjects.createObjectBuilder(eventPayload)
                .add(COURT_HOUSE_CODE_KEY, courtCodeForName)
                .build();
    }

    @Override
    public void setEnveloper(Enveloper enveloper) {
        this.enveloper = enveloper;
    }

    abstract String getEventName();

    abstract String getEventSchemaFileName();

    abstract Logger getLogger();

}
