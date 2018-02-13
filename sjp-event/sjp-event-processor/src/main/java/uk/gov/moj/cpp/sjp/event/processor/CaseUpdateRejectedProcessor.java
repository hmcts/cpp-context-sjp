package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseUpdateRejectedProcessor {

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("sjp.events.case-update-rejected")
    public void caseUpdateRejected(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final JsonObject newPayload = createObjectBuilder()
                .add(EventProcessorConstants.CASE_ID, payload.getString(EventProcessorConstants.CASE_ID))
                .add("reason", payload.getString("reason"))
                .build();
        final JsonEnvelope newEventEnvelope = enveloper.withMetadataFrom(event,
                "public.sjp.case-update-rejected").apply(newPayload);
        sender.send(newEventEnvelope);
    }
}
