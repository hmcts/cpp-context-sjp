package uk.gov.moj.cpp.sjp;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import javax.json.JsonObject;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

@Transformation
public class FinancialMeansUpdatedEventTransformer extends BaseEventTransformer {



    protected static final String EMPLOYER = "employer";

    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        if (!getEventName().equalsIgnoreCase(eventEnvelope.metadata().name())) {
            return NO_ACTION;
        }

        final JsonObject payload = eventEnvelope.payloadAsJsonObject();
        return containsAddressWithPostcodeToTransform(ADDRESS, EMPLOYER, payload) ? TRANSFORM : NO_ACTION;
    }


    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope eventEnvelope) {
        JsonObject payload = eventEnvelope.payloadAsJsonObject();
        payload = checkAndTransformObject(EMPLOYER, payload);

        final JsonEnvelope transformedEnvelope = envelopeFrom(
                eventEnvelope.metadata(),
                payload);

        return of(transformedEnvelope);
    }


    @Override
    public String getEventName() {
        return "sjp.events.all-financial-means-updated";
    }
}
