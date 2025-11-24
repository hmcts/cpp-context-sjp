package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantPendingChangesAcceptedProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantPendingChangesAcceptedProcessor.class);

    @Inject
    private Enveloper enveloper;
    @Inject
    private Sender sender;

    @Handles("sjp.events.defendant-pending-changes-accepted")
    public void publish(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("Sending public.sjp.events.defendant-pending-changes-accepted for caseId: {}", jsonEnvelope.payloadAsJsonObject().getString("caseId"));

        sender.send(enveloper.withMetadataFrom(jsonEnvelope,
                        "public.sjp.events.defendant-pending-changes-accepted").
                apply(jsonEnvelope.payloadAsJsonObject()));
    }
}
