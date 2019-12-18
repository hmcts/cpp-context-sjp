package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DATES_TO_AVOID;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class DatesToAvoidProcessor {

    public static final String DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME = "public.sjp.dates-to-avoid-added";
    public static final String DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME = "public.sjp.dates-to-avoid-updated";

    @Inject
    private Sender sender;

    @Inject
    private CaseStateService caseStateService;

    @Inject
    private TimerService timerService;

    @Handles("sjp.events.dates-to-avoid-added")
    public void publishDatesToAvoidAdded(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName(DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME)
                .withMetadataFrom(envelope));

        updateActivityForExistingCasesUsingTheLegacyFlow(envelope);
    }

    @Handles("sjp.events.dates-to-avoid-updated")
    public void publishDatesToAvoidUpdated(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName(DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME)
                .withMetadataFrom(envelope));
    }

    @Handles(DatesToAvoidRequired.EVENT_NAME)
    public void datesToAvoidRequired(final JsonEnvelope event) {
        final JsonObject eventPayload = event.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(eventPayload.getString("caseId"));
        final LocalDate datesToAvoidExpirationDate = LocalDates.from(eventPayload.getString("datesToAvoidExpirationDate"));

        timerService.startTimerForDatesToAvoid(caseId, datesToAvoidExpirationDate, event.metadata());
    }


    private void updateActivityForExistingCasesUsingTheLegacyFlow(final JsonEnvelope envelope) {
        final JsonObject datesToAvoidAddedPayload = envelope.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(datesToAvoidAddedPayload.getString(CASE_ID));
        final String datesToAvoid = datesToAvoidAddedPayload.getString(DATES_TO_AVOID);

        caseStateService.datesToAvoidAdded(caseId, datesToAvoid, envelope.metadata());
    }

}
