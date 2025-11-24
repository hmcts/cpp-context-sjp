package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.casemanagement.UpdateCasesManagementStatus;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_PROCESSOR)
public class UpdateCasesManagementStatusProcessor {

    @Inject
    private Enveloper enveloper;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    private static final String PUBLIC_UPDATE_CASES_MANAGEMENT_STATUS_EVENT = "public.sjp.cases-management-status-updated";

    @SuppressWarnings("squid:S00112")
    @Handles(UpdateCasesManagementStatus.EVENT_NAME)
    @Transactional
    public void updateCasesManagementStatus(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        payload.getJsonArray("cases").getValuesAs(JsonObject.class)
                .forEach(casePayload -> sender.send(envelop(casePayload)
                        .withName("sjp.command.change-case-management-status")
                        .withMetadataFrom(envelope)));

        sender.send(envelop(payload)
                .withName(PUBLIC_UPDATE_CASES_MANAGEMENT_STATUS_EVENT)
                .withMetadataFrom(envelope));
    }
}
