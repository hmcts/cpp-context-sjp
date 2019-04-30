package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class DecisionProcessor {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("public.resulting.referenced-decisions-saved")
    public void handelDecisionsSaved(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.complete-case").apply(createObjectBuilder()
                .add(CASE_ID, envelope.payloadAsJsonObject().getString(CASE_ID))
                .build()));
    }
}
