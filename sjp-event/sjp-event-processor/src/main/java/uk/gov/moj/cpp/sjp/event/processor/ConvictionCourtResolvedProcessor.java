package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.decision.ConvictionCourtResolved;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class ConvictionCourtResolvedProcessor {

    @Inject
    private Sender sender;

    public static final String PUBLIC_CONVICTION_COURT_RESOLVED = "public.sjp.events.conviction-court-resolved";

    @Handles(ConvictionCourtResolved.EVENT_NAME)
    public void handleResolveConvictionCourt(final JsonEnvelope convictionCourtResolvedEnvelope) {
        final JsonObject resolvedConvictionCourt = convictionCourtResolvedEnvelope.payloadAsJsonObject();

        sender.send(envelop(resolvedConvictionCourt)
                .withName(PUBLIC_CONVICTION_COURT_RESOLVED)
                .withMetadataFrom(convictionCourtResolvedEnvelope));

    }
}
