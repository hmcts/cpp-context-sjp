package uk.gov.moj.cpp.sjp.domain.transformation.datecreated;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.time.ZonedDateTime;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class DelegatedPowersSessionStartedEventTransformer extends BaseEventTransformer {

    private static final Logger LOGGER
            = LoggerFactory.getLogger(DelegatedPowersSessionStartedEventTransformer.class);

    private static final String STARTED_AT = "startedAt";

    public DelegatedPowersSessionStartedEventTransformer() {
    }

    @Override
    String getEventName() {
        return "sjp.events.delegated-powers-session-started";
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    public ZonedDateTime returnTransformedCreatedAt(final JsonEnvelope event) {
        JsonObject eventPayload = event.payloadAsJsonObject();

        return ZonedDateTime.parse(eventPayload.getString(STARTED_AT));
    }
}
