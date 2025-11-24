package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.LocalDate.parse;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseAdjournmentProcessor {

    @Inject
    private TimerService timerService;

    @Handles("sjp.events.case-adjourned-to-later-sjp-hearing-recorded")
    public void caseAdjournedForLaterSjpHearingRecorded(final JsonEnvelope event) {
        final JsonObject adjournmentDetails = event.payloadAsJsonObject();

        final UUID caseId = fromString(adjournmentDetails.getString("caseId"));
        final LocalDate adjournedTo = parse(adjournmentDetails.getString("adjournedTo"));

        timerService.startTimerForAdjournmentToDay(caseId, adjournedTo, event.metadata());
    }
}
