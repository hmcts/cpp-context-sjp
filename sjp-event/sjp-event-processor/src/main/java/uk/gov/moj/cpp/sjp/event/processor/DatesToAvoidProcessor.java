package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DATES_TO_AVOID;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class DatesToAvoidProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private CaseStateService caseStateService;

    @Handles("sjp.events.dates-to-avoid-added")
    public void publishDatesToAvoidAdded(final JsonEnvelope envelope) {
        final JsonObject datesToAvoidAddedPayload = envelope.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(datesToAvoidAddedPayload.getString(CASE_ID));
        final String datesToAvoid = datesToAvoidAddedPayload.getString(DATES_TO_AVOID);

        caseStateService.datesToAvoidAdded(caseId, datesToAvoid, envelope.metadata());
    }

    @Handles("sjp.events.dates-to-avoid-updated")
    public void publishDatesToAvoidUpdated(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "public.sjp.dates-to-avoid-updated")
                .apply(envelope.payloadAsJsonObject()));
    }

}
