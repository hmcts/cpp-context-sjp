package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.ENTERPRISE_ID;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class EnterpriseIdAssociatedProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.events.enterprise-id-associated")
    public void enterpriseIdAssociated(final JsonEnvelope event) {
        final JsonObject payload = buildPublicPayloadFrom(event.payloadAsJsonObject());

        final JsonEnvelope publicEvent = enveloper.withMetadataFrom(event, "public.sjp.enterprise-id-associated").apply(payload);

        sender.send(publicEvent);
    }

    private JsonObject buildPublicPayloadFrom(final JsonObject privateEventPayload) {
        return createObjectBuilder()
                .add(CASE_ID, privateEventPayload.getString(CASE_ID))
                .add(ENTERPRISE_ID, privateEventPayload.getString(ENTERPRISE_ID))
                .build();
    }
}