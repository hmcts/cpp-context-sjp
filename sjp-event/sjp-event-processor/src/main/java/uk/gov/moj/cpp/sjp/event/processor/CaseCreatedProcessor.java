package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(EVENT_PROCESSOR)
public class CaseCreatedProcessor {

    private static final String SJP_CASE_CREATED_PUBLIC_EVENT = "public.structure.sjp-case-created";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.events.sjp-case-created")
    public void publishSjpCaseCreatedPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString("id");
        final String postingDate = event.payloadAsJsonObject().getString("postingDate");
        final JsonObject payload = Json.createObjectBuilder().add("id", caseId).add("postingDate", postingDate).build();
        sender.send(enveloper.withMetadataFrom(event, SJP_CASE_CREATED_PUBLIC_EVENT).apply(payload));
    }
}
