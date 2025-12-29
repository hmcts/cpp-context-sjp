package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.REASON;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseUpdateRejectedProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles(CaseUpdateRejected.EVENT_NAME)
    public void caseUpdateRejected(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final JsonObject newPayload = createObjectBuilder()
                .add(CASE_ID, payload.getString(CASE_ID))
                .add(REASON, payload.getString(REASON))
                .build();
        final JsonEnvelope newEventEnvelope = enveloper.withMetadataFrom(event,
                "public.sjp.case-update-rejected").apply(newPayload);
        sender.send(newEventEnvelope);
    }
}
