package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantDetailsUpdateProcessor {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.events.defendant-details-updated")
    public void publish(final JsonEnvelope jsonEnvelope) {
        sender.send(enveloper.withMetadataFrom(jsonEnvelope,
                "public.sjp.events.defendant-details-updated").
                apply(jsonEnvelope.payloadAsJsonObject()));
    }
}
