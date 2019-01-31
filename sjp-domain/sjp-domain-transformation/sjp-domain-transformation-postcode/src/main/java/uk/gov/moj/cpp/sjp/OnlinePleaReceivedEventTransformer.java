package uk.gov.moj.cpp.sjp;

import static java.util.stream.Stream.of;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.stream.Stream;

import javax.json.JsonObject;

@Transformation
public class OnlinePleaReceivedEventTransformer extends BaseEventTransformer {

    protected static final String PERSONAL_DETAILS = "personalDetails";
    protected static final String EMPLOYER = "employer";

    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        if (!getEventName().equalsIgnoreCase(eventEnvelope.metadata().name())) {
            return NO_ACTION;
        }

        final JsonObject payload = eventEnvelope.payloadAsJsonObject();
        return containsAddressWithPostcodeToTransform(ADDRESS, PERSONAL_DETAILS, payload)
                || containsAddressWithPostcodeToTransform(ADDRESS, EMPLOYER, payload) ? TRANSFORM : NO_ACTION;
    }


    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope eventEnvelope) {
        JsonObject payload = eventEnvelope.payloadAsJsonObject();
        payload = checkAndTransformObject(PERSONAL_DETAILS, payload);
        payload = checkAndTransformObject(EMPLOYER, payload);

        final JsonEnvelope transformedEnvelope = envelopeFrom(
                eventEnvelope.metadata(),
                payload);

        return of(transformedEnvelope);
    }

    @Override
    public String getEventName() {
        return "sjp.events.online-plea-received";
    }
}
