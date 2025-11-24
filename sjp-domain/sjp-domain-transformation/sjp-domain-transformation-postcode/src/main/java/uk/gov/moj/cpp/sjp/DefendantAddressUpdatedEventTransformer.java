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
public class DefendantAddressUpdatedEventTransformer extends BaseEventTransformer {

    protected static final String NEW_ADDRESS = "newAddress";
    protected static final String OLD_ADDRESS = "oldAddress";

    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        if (!getEventName().equalsIgnoreCase(eventEnvelope.metadata().name())) {
            return NO_ACTION;
        }

        final JsonObject payload = eventEnvelope.payloadAsJsonObject();
        return containsPostcodeToTransform(NEW_ADDRESS, payload)
                || containsPostcodeToTransform(OLD_ADDRESS, payload) ? TRANSFORM : NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope eventEnvelope) {
        final JsonObject payload = eventEnvelope.payloadAsJsonObject();
        final JsonEnvelope transformedEnvelope = envelopeFrom(
                eventEnvelope.metadata(),
                replacePostcodeInAddress(replacePostcodeInAddress(payload, OLD_ADDRESS), NEW_ADDRESS));

        return of(transformedEnvelope);
    }

    @Override
    public String getEventName() {
        return "sjp.events.defendant-address-updated";
    }

}
