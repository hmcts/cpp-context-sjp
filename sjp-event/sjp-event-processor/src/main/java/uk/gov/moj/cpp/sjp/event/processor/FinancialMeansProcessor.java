package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class FinancialMeansProcessor {

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("structure.events.financial-means-updated")
    public void updateFinancialMeans(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "public.structure.financial-means-updated")
                .apply(envelope.payloadAsJsonObject()));
    }
}
