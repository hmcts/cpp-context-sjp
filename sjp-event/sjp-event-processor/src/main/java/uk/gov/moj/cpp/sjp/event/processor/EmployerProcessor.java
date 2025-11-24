package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class EmployerProcessor {

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("sjp.events.employer-updated")
    public void updateEmployer(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "public.sjp.employer-updated")
                .apply(envelope.payloadAsJsonObject()));
    }

    @Handles("sjp.events.employer-deleted")
    public void deleteEmployer(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "public.sjp.employer-deleted")
                .apply(envelope.payloadAsJsonObject()));
    }

}
