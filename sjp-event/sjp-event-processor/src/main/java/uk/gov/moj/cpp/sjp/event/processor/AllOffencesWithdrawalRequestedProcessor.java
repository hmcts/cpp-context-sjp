package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.EVENT_PROCESSOR)
public class AllOffencesWithdrawalRequestedProcessor {

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;

    @Handles("sjp.events.all-offences-withdrawal-requested")
    public void publishAllOffencesWithdrawalEvent(final JsonEnvelope event) {
        final JsonEnvelope newEventEnvelope = enveloper.withMetadataFrom(event,
                "public.sjp.all-offences-withdrawal-requested")
                .apply(createObjectBuilder()
                        .add(CASE_ID, event.payloadAsJsonObject().getString(CASE_ID))
                        .build());
        sender.send(newEventEnvelope);
    }
}
