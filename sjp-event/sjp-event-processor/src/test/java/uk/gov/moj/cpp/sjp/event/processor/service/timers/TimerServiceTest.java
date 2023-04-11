package uk.gov.moj.cpp.sjp.event.processor.service.timers;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import java.time.ZonedDateTime;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.activiti.TimerExpirationProcess;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TimerServiceTest {

    @Mock
    private TimerExpirationProcess process;

    @InjectMocks
    private TimerService service;

    private UUID caseId;

    private Metadata metadata;

    @Before
    public void setup() {
        this.caseId = randomUUID();
        this.metadata = metadataWithRandomUUIDAndName().build();
    }

    @Test
    public void shouldStartDefendantResponseExpirationTimer() {
        final LocalDate expectedDateReady = LocalDate.now().plusDays(28);
        service.startTimerForDefendantResponse(caseId, expectedDateReady, metadata);

        verify(process).startTimerForDelayAndCommand(
                this.caseId,
                expectedDateReady,
                TimerService.DEFENDANT_RESPONSE_TIMER_COMMAND,
                metadata);
    }

    @Test
    public void shouldStartDatesToAvoidExpirationTimer() {
        final LocalDate expectedDateReady = LocalDate.now().plusDays(10);
        service.startTimerForDatesToAvoid(caseId, expectedDateReady, metadata);

        verify(process).startTimerForDelayAndCommand(
                this.caseId,
                expectedDateReady,
                TimerService.DATES_TO_AVOID_RESPONSE_TIMER_COMMAND,
                metadata);
    }

    @Test
    public void shouldStartAdjournmentTimer() {
        final LocalDate adjournmentTo = LocalDate.now().plusDays(15);
        service.startTimerForAdjournmentToDay(caseId, adjournmentTo, metadata);

        verify(process).startTimerForDelayAndCommand(
                this.caseId,
                adjournmentTo,
                TimerService.ADJOURNMENT_TIMER_COMMAND,
                metadata);
    }

    @Test
    public void shouldStartDefendantAocpAcceptanceTimer() {
        final LocalDate expectedDateReady = LocalDate.now();//.plusDays(5);
        service.startTimerForDefendantAOCPAcceptance(caseId, expectedDateReady, metadata);

        verify(process).startTimerForDelayAndCommand(
                this.caseId,
                expectedDateReady,
                TimerService.DEFENDANT_AOCP_RESPONSE_TIMER_COMMAND,
                metadata);
    }

    @Test
    public void shouldStartUndoReserveCaseTimer() {
        final ZonedDateTime expectedDateReady = ZonedDateTime.now().plusDays(1);
        service.startTimerForUndoReserveCase(caseId, expectedDateReady, metadata);

        verify(process).startTimerForDelayAndCommand(
                this.caseId,
                expectedDateReady,
                TimerService.UNDO_RESERVE_CASE_TIMER_COMMAND,
                metadata);
    }
}