package uk.gov.moj.cpp.sjp.event.processor;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseCreationFailedBecauseCaseAlreadyExistedProcessor {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.events.case-creation-failed-because-case-already-existed")
    public void publish(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        sender.send(enveloper.withMetadataFrom(event,
                "public.sjp.case-creation-failed-because-case-already-existed").apply(payload));
    }
}
