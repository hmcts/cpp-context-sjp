package uk.gov.moj.cpp.sjp.event.processor.service.timers;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.activiti.TimerExpirationProcess;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;

public class TimerService {

    public static final String DEFENDANT_RESPONSE_TIMER_COMMAND = "sjp.command.expire-defendant-response-timer";
    public static final String DATES_TO_AVOID_RESPONSE_TIMER_COMMAND = "sjp.command.expire-dates-to-avoid-timer";
    public static final String ADJOURNMENT_TIMER_COMMAND = "sjp.command.record-case-adjournment-to-later-sjp-hearing-elapsed";
    public static final String DEFENDANT_AOCP_RESPONSE_TIMER_COMMAND = "sjp.expire-defendant-aocp-response-timer";

    @Inject
    private TimerExpirationProcess process;

    public void startTimerForDefendantResponse(final UUID caseId, final LocalDate expirationDate, final Metadata metadata) {
        process.startTimerForDelayAndCommand(caseId, expirationDate, DEFENDANT_RESPONSE_TIMER_COMMAND, metadata);
    }

    public void startTimerForDatesToAvoid(final UUID caseId, final LocalDate expirationDate, final Metadata metadata) {
        process.startTimerForDelayAndCommand(caseId, expirationDate, DATES_TO_AVOID_RESPONSE_TIMER_COMMAND, metadata);
    }

    public void startTimerForAdjournmentToDay(final UUID caseId, final LocalDate expirationDate, final Metadata metadata) {
        process.startTimerForDelayAndCommand(caseId, expirationDate, ADJOURNMENT_TIMER_COMMAND, metadata);
    }

    public void startTimerForDefendantAOCPAcceptance(final UUID caseId, final LocalDate expirationDate, final Metadata metadata) {
        process.startTimerForDelayAndCommand(caseId, expirationDate, DEFENDANT_AOCP_RESPONSE_TIMER_COMMAND, metadata);
    }
}
