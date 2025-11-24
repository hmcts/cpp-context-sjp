package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static java.time.LocalDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.NOTICE_ENDED_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDateTime;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExpectedDateReadyCalculatorTest {

    @Mock
    private DelegateExecution delegateExecution;

    private final ExpectedDateReadyCalculator expectedDateReadyCalculator = new ExpectedDateReadyCalculator();

    @Test
    public void shouldReturnDefendantResponseTimerWhenCaseNotAdjournedNorWaitingForDatesToAvoid() {
        final LocalDateTime defendantResponseTimer = now();

        givenCaseReceived(defendantResponseTimer);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution), is(defendantResponseTimer));
    }

    @Test
    public void shouldReturnAdjournmentTimerWhenPleaIsPresentAndDefendantResponseTimerNotExpired() {
        final LocalDateTime defendantResponseTimerExpiration = now().plusDays(10);
        final LocalDateTime adjournmentTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenPleaUpdated();
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution), is(adjournmentTimerExpiration));
    }

    @Test
    public void shouldReturnAdjournmentDateWhenPleaIsPresentAndDefendantResponseTimerExpired() {
        final LocalDateTime defendantResponseTimerExpiration = now().minusDays(1);
        final LocalDateTime adjournmentTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenPleaUpdated();
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution), is(adjournmentTimerExpiration));
    }

    @Test
    public void shouldReturnDefendantTimerWhenCaseIsAdjournedAndPleaIsNotPresentAndDefendantResponseTimerExpireAfterAdjournmentDate() {
        final LocalDateTime defendantResponseTimerExpiration = now().plusDays(10);
        final LocalDateTime adjournmentTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution), is(defendantResponseTimerExpiration));
    }

    @Test
    public void shouldReturnAdjournmentDateWhenPleaIsNotPresentAndDefendantResponseTimerElapseBeforeAdjournmentDate() {
        final LocalDateTime defendantResponseTimerExpiration = now().plusDays(10);
        final LocalDateTime adjournmentTimerExpiration = now().plusDays(14);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution), is(adjournmentTimerExpiration));
    }

    @Test
    public void shouldReturnDatesToAvoidTimerWhenCaseNotAdjournedAndDefendantResponseTimerNotExpired() {
        final LocalDateTime defendantResponseTimerExpiration = now().plusDays(10);
        final LocalDateTime datesToAvoidTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        when(delegateExecution.getVariable(CASE_ADJOURNED_VARIABLE, Boolean.class)).thenReturn(false);
        when(delegateExecution.getVariable(PLEA_READY_VARIABLE, Boolean.class)).thenReturn(false);
        when(delegateExecution.getVariable(PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE, String.class)).thenReturn(datesToAvoidTimerExpiration.toString());

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution), is(datesToAvoidTimerExpiration));
    }

    @Test
    public void shouldReturnDatesToAvoidTimerWhenCaseNotAdjournedAndDefendantResponseTimerExpired() {
        final LocalDateTime defendantResponseTimerExpiration = now().minusDays(1);
        final LocalDateTime datesToAvoidTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        when(delegateExecution.getVariable(CASE_ADJOURNED_VARIABLE, Boolean.class)).thenReturn(false);
        when(delegateExecution.getVariable(PLEA_READY_VARIABLE, Boolean.class)).thenReturn(false);
        when(delegateExecution.getVariable(PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE, String.class)).thenReturn(datesToAvoidTimerExpiration.toString());

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution), is(datesToAvoidTimerExpiration));
    }

    @Test
    public void shouldReturnDatesToAvoidTimerWhenItExpireLaterThanAdjournment() {
        final LocalDateTime defendantResponseTimerExpiration = now().plusDays(20);
        final LocalDateTime datesToAvoidTimerExpiration = now().plusDays(10);
        final LocalDateTime adjournmentTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        when(delegateExecution.getVariable(PLEA_READY_VARIABLE, Boolean.class)).thenReturn(false);
        when(delegateExecution.getVariable(PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE, String.class)).thenReturn(datesToAvoidTimerExpiration.toString());
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution), is(datesToAvoidTimerExpiration));
    }

    @Test
    public void shouldReturnAdjournmentTimerWhenItExpireAfterDatesToAvoidTimer() {
        final LocalDateTime defendantResponseTimerExpiration = now().plusDays(20);
        final LocalDateTime datesToAvoidTimerExpiration = now().plusDays(10);
        final LocalDateTime adjournmentTimerExpiration = now().plusDays(14);

        givenCaseReceived(defendantResponseTimerExpiration);
        when(delegateExecution.getVariable(PLEA_READY_VARIABLE, Boolean.class)).thenReturn(false);
        when(delegateExecution.getVariable(PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE, String.class)).thenReturn(datesToAvoidTimerExpiration.toString());
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(delegateExecution), is(adjournmentTimerExpiration));
    }

    private void givenCaseReceived(final LocalDateTime defendantResponseTimerExpiration) {
        when(delegateExecution.getVariable(NOTICE_ENDED_DATE_VARIABLE, String.class)).thenReturn(defendantResponseTimerExpiration.toString());
    }

    private void givenPleaUpdated() {
        when(delegateExecution.getVariable(PLEA_TYPE_VARIABLE, String.class)).thenReturn(PleaType.GUILTY.name());
        when(delegateExecution.getVariable(PLEA_READY_VARIABLE, Boolean.class)).thenReturn(true);
    }

    private void givenCaseAdjourned(final LocalDateTime adjournmentTimerExpiration) {
        when(delegateExecution.getVariable(CASE_ADJOURNED_VARIABLE, Boolean.class)).thenReturn(true);
        when(delegateExecution.getVariable(CASE_ADJOURNED_DATE, String.class)).thenReturn(adjournmentTimerExpiration.toString());
    }

}
