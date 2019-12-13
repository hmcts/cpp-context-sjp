package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.PleasSet;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class PleasSetProcessor {

    @Inject
    private Sender sender;

    @Handles(PleasSet.EVENT_NAME)
    public void publishPleasSet(final JsonEnvelope jsonEnvelope) {
        sender.send(envelop(jsonEnvelope.payloadAsJsonObject())
                .withName("public.sjp.pleas-set")
                .withMetadataFrom(jsonEnvelope));
    }
}
