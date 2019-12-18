package uk.gov.moj.cpp.sjp.domain.aggregate.casestatus;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_PLEA;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExpectedDateReadyCalculatorTest {

    private final ExpectedDateReadyCalculator expectedDateReadyCalculator = new ExpectedDateReadyCalculator();
    private final CaseAggregateState state = new CaseAggregateState();

    @Test
    public void shouldReturnDefendantResponseTimerWhenCaseNotAdjournedNorWaitingForDatesToAvoid() {
        final LocalDate defendantResponseTimer = now();

        givenCaseReceived(defendantResponseTimer);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(defendantResponseTimer));
    }

    @Test
    public void shouldReturnAdjournmentTimerWhenPleaIsPresentAndDefendantResponseTimerNotExpired() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(7);
        final LocalDate adjournmentTimerExpiration = now().plusDays(10);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenNotGuiltyPleaUpdated();
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(adjournmentTimerExpiration));
    }

    @Test
    public void shouldReturnAdjournmentDateWhenPleaIsPresentAndDefendantResponseTimerExpired() {
        final LocalDate defendantResponseTimerExpiration = now().minusDays(1);
        final LocalDate adjournmentTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenNotGuiltyPleaUpdated();
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(adjournmentTimerExpiration));
    }

    @Test
    public void shouldReturnDefendantTimerWhenCaseIsAdjournedAndPleaIsNotPresentAndDefendantResponseTimerExpireAfterAdjournmentDate() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(10);
        final LocalDate adjournmentTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(defendantResponseTimerExpiration));
    }

    @Test
    public void shouldReturnAdjournmentDateWhenPleaIsNotPresentAndDefendantResponseTimerElapseBeforeAdjournmentDate() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(10);
        final LocalDate adjournmentTimerExpiration = now().plusDays(14);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(adjournmentTimerExpiration));
    }

    @Test
    public void shouldReturnDatesToAvoidTimerWhenCaseNotAdjournedAndDefendantResponseTimerNotExpired() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(7);
        final LocalDate datesToAvoidTimerExpiration = now().plusDays(10);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenNotGuiltyCaseWaitingForDatesToAvoid(datesToAvoidTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(datesToAvoidTimerExpiration));
    }

    @Test
    public void shouldReturnDatesToAvoidTimerWhenCaseNotAdjournedAndDefendantResponseTimerNotExpiredAndWithoutNotGuiltyPlea() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(7);
        final LocalDate datesToAvoidTimerExpiration = now().plusDays(10);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenGuiltyCaseWaitingForDatesToAvoid(datesToAvoidTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(defendantResponseTimerExpiration));
    }

    @Test
    public void shouldReturnDatesToAvoidTimerWhenCaseNotAdjournedAndDefendantResponseTimerExpired() {
        final LocalDate defendantResponseTimerExpiration = now().minusDays(1);
        final LocalDate datesToAvoidTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenNotGuiltyCaseWaitingForDatesToAvoid(datesToAvoidTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(datesToAvoidTimerExpiration));
    }

    @Test
    public void shouldReturnDefendantResponseTimerWhenCaseNotAdjournedAndDatesToAvoidTimerExpiredAndDatesToAvoidNotProvided() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(1);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenDatesToAvoidExpiredAndNotProvided();

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(defendantResponseTimerExpiration));
    }

    @Test
    public void shouldReturnDatesToAvoidTimerWhenItExpireLaterThanAdjournment() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(1);
        final LocalDate datesToAvoidTimerExpiration = now().plusDays(10);
        final LocalDate adjournmentTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenNotGuiltyCaseWaitingForDatesToAvoid(datesToAvoidTimerExpiration);
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(datesToAvoidTimerExpiration));
    }

    @Test
    public void shouldReturnAdjournmentTimerWhenDatesToAvoidAreProvidedAndDatesToAvoidNotExpired() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(1);
        final LocalDate datesToAvoidTimerExpiration = now().plusDays(10);
        final LocalDate adjournmentTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenDatesToAvoidProvided(datesToAvoidTimerExpiration);
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(adjournmentTimerExpiration));
    }

    @Test
    public void shouldReturnAdjournmentTimerWhenItExpireAfterDatesToAvoidTimer() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(1);
        final LocalDate datesToAvoidTimerExpiration = now().plusDays(10);
        final LocalDate adjournmentTimerExpiration = now().plusDays(14);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenNotGuiltyCaseWaitingForDatesToAvoid(datesToAvoidTimerExpiration);
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(adjournmentTimerExpiration));
    }

    @Test
    public void shouldReturnDatesToAvoidTimerWhenItExpireAfterAdjournmentButBeforeDefendantResponseTimer() {
        final LocalDate defendantResponseTimerExpiration = now().plusDays(15);
        final LocalDate datesToAvoidTimerExpiration = now().plusDays(10);
        final LocalDate adjournmentTimerExpiration = now().plusDays(7);

        givenCaseReceived(defendantResponseTimerExpiration);
        givenNotGuiltyCaseWaitingForDatesToAvoid(datesToAvoidTimerExpiration);
        givenCaseAdjourned(adjournmentTimerExpiration);

        assertThat(expectedDateReadyCalculator.calculateExpectedDateReady(state), is(datesToAvoidTimerExpiration));
    }

    private void givenCaseReceived(final LocalDate defendantResponseTimerExpiration) {
        state.setPostingDate(defendantResponseTimerExpiration.minusDays(NUMBER_DAYS_WAITING_FOR_PLEA));
    }

    private void givenNotGuiltyPleaUpdated() {
        state.setPleas(newArrayList(new Plea(randomUUID(), randomUUID(), NOT_GUILTY)));
    }

    private void givenCaseAdjourned(final LocalDate adjournmentTimerExpiration) {
        state.setAdjournedTo(adjournmentTimerExpiration);
    }

    private void givenNotGuiltyCaseWaitingForDatesToAvoid(final LocalDate datesToAvoidTimerExpiration) {
        givenCaseWaitingForDatesToAvoid(datesToAvoidTimerExpiration, NOT_GUILTY);
    }

    private void givenGuiltyCaseWaitingForDatesToAvoid(final LocalDate datesToAvoidTimerExpiration) {
        givenCaseWaitingForDatesToAvoid(datesToAvoidTimerExpiration, GUILTY);
    }

    private void givenCaseWaitingForDatesToAvoid(final LocalDate datesToAvoidTimerExpiration, final PleaType pleaType) {
        state.setPleas(newArrayList(new Plea(randomUUID(), randomUUID(), pleaType)));
        state.setDatesToAvoidExpirationDate(datesToAvoidTimerExpiration);
        state.setDatesToAvoidPreviouslyRequested();
    }

    private void givenDatesToAvoidProvided(final LocalDate datesToAvoidTimerExpiration) {
        givenNotGuiltyCaseWaitingForDatesToAvoid(datesToAvoidTimerExpiration);
        state.setDatesToAvoid("dates to avoid are provided");
    }

    private void givenDatesToAvoidExpiredAndNotProvided() {
        state.setDatesToAvoidPreviouslyRequested();
        state.datesToAvoidTimerExpired();
        state.setDatesToAvoid(null);
    }
}