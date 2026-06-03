package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAdjournmentProcessorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final LocalDate ADJOURNED_TO = LocalDate.now();

    @Mock
    private CaseStateService caseStateService;

    @Mock
    private TimerService timerService;

    @InjectMocks
    private CaseAdjournmentProcessor caseAdjournmentProcessor;

    @Test
    public void shouldInvokeActivityFlowWhenCaseAdjournmentRecorder() {
        final JsonEnvelope caseAdjournmentRecordedEvent = EnvelopeFactory
                .createEnvelope("sjp.events.case-adjourned-to-later-sjp-hearing-recorded",
                        createObjectBuilder()
                                .add("caseId", CASE_ID.toString())
                                .add("adjournedTo", ADJOURNED_TO.toString())
                                .build());

        caseAdjournmentProcessor.caseAdjournedForLaterSjpHearingRecorded(caseAdjournmentRecordedEvent);

        verify(timerService).startTimerForAdjournmentToDay(CASE_ID, ADJOURNED_TO, caseAdjournmentRecordedEvent.metadata());
        verify(caseStateService, never()).caseAdjournedForLaterHearing(CASE_ID, ADJOURNED_TO.atStartOfDay(), caseAdjournmentRecordedEvent.metadata());
    }
}
