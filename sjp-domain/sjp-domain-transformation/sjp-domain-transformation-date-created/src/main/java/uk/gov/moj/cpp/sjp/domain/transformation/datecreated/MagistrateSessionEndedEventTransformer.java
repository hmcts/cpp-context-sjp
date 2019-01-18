package uk.gov.moj.cpp.sjp.domain.transformation.datecreated;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.time.ZonedDateTime;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class MagistrateSessionEndedEventTransformer extends BaseEventTransformer {

    private static final Logger LOGGER
            = LoggerFactory.getLogger(MagistrateSessionEndedEventTransformer.class);

    private static final String ENDED_AT = "endedAt";

    @Override
    String getEventName() {
        return "sjp.events.magistrate-session-ended";
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    public ZonedDateTime returnTransformedCreatedAt(final JsonEnvelope event) {
        JsonObject eventPayload = event.payloadAsJsonObject();

        return ZonedDateTime.parse(eventPayload.getString(ENDED_AT));
    }
}
