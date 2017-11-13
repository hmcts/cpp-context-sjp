package uk.gov.moj.cpp.sjp.event.processor.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(EVENT_PROCESSOR)
public class PleaUpdatedProcessor {

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("sjp.events.plea-updated")
    public void updatePlea(final JsonEnvelope envelope) {
        publishPublicEvent(envelope, "public.structure.plea-updated");
    }

    @Handles("sjp.events.plea-cancelled")
    public void cancelPlea(final JsonEnvelope envelope) {
        publishPublicEvent(envelope, "public.structure.plea-cancelled");
    }

    private void publishPublicEvent(final JsonEnvelope envelope, final String eventName) {
        final JsonObject event = envelope.payloadAsJsonObject();
        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add("caseId", event.getString("caseId"))
                .add("offenceId", event.getString("offenceId"));
        if (event.containsKey("plea")) {
            payloadBuilder.add("plea", event.getString("plea"));
        }
        sender.send(enveloper.withMetadataFrom(envelope, eventName).apply(payloadBuilder.build()));
    }
}
