package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseExpectedDateReadyChanged;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUpdated;
import uk.gov.moj.cpp.sjp.event.CaseStarted;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidAdded;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidUpdated;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.decommissioned.CaseAssignmentDeleted;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.Test;

public class CompositeCaseAggregateStateMutatorTest {

    private final UUID caseId = randomUUID();
    private final UUID userId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID offenceId = randomUUID();
    private final CaseAggregateState caseAggregateState = new CaseAggregateState();
    private final CompositeCaseAggregateStateMutator compositeCaseAggregateStateMutator = CompositeCaseAggregateStateMutator.INSTANCE;

    @Test
    public void shouldMutateStateOnAllOffencesWithdrawalRequestedEvent() {
        final AllOffencesWithdrawalRequested allOffencesWithdrawalRequested = new AllOffencesWithdrawalRequested(caseId);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(allOffencesWithdrawalRequested, caseAggregateState);

        assertTrue(caseAggregateState.isWithdrawalAllOffencesRequested());
    }

    @Test
    public void shouldMutateStateOnAllOffencesWithdrawalRequestCancelledEvent() {
        final AllOffencesWithdrawalRequestCancelled allOffencesWithdrawalRequestCancelled = new AllOffencesWithdrawalRequestCancelled(caseId);

        compositeCaseAggregateStateMutator.apply(allOffencesWithdrawalRequestCancelled, caseAggregateState);

        assertFalse(caseAggregateState.isWithdrawalAllOffencesRequested());
    }

    @Test
    public void shouldMutateStateOnCaseAssignedEvent() {
        final CaseAssigned caseAssigned = new CaseAssigned(caseId, userId, now(), CaseAssignmentType.MAGISTRATE_DECISION);

        compositeCaseAggregateStateMutator.apply(caseAssigned, caseAggregateState);

        assertTrue(caseAggregateState.isAssignee(userId));
    }

    @Test
    public void shouldMutateStateOnCaseUnassignedEvent() {
        caseAggregateState.setAssigneeId(userId);

        final CaseUnassigned caseUnassigned = new CaseUnassigned(caseId);

        compositeCaseAggregateStateMutator.apply(caseUnassigned, caseAggregateState);

        assertFalse(caseAggregateState.isAssignee(userId));
    }

    @Test
    public void shouldMutateStateOnCaseAssignmentDeletedEvent() {
        caseAggregateState.setAssigneeId(userId);

        final CaseAssignmentDeleted caseAssignmentDeleted = new CaseAssignmentDeleted(caseId, CaseAssignmentType.MAGISTRATE_DECISION);

        compositeCaseAggregateStateMutator.apply(caseAssignmentDeleted, caseAggregateState);

        assertFalse(caseAggregateState.isAssignee(userId));
    }

    @Test
    public void shouldMutateStateOnCaseMarkedReadyForDecisionEvent() {
        caseAggregateState.setCaseReceived(true);

        final CaseReadinessReason readinessReason = CaseReadinessReason.PLEADED_GUILTY;
        final CaseMarkedReadyForDecision caseMarkedReadyForDecision = new CaseMarkedReadyForDecision(caseId, readinessReason, now());

        compositeCaseAggregateStateMutator.apply(caseMarkedReadyForDecision, caseAggregateState);

        assertThat(caseAggregateState.getReadinessReason(), is(readinessReason));
    }

    @Test
    public void shouldMutateStateOnCaseUnmarkedReadyForDecisionEvent() {
        caseAggregateState.setCaseReceived(true);

        final CaseUnmarkedReadyForDecision caseUnmarkedReadyForDecision = new CaseUnmarkedReadyForDecision(caseId, LocalDate.now());

        compositeCaseAggregateStateMutator.apply(caseUnmarkedReadyForDecision, caseAggregateState);

        assertNull(caseAggregateState.getReadinessReason());
        assertFalse(caseAggregateState.isCaseReadyForDecision());
        assertThat(caseAggregateState.getExpectedDateReady(), is(caseUnmarkedReadyForDecision.getExpectedDateReady()));
    }

    @Test
    public void shouldMutateStateOnCaseExpectedDateReadyChangedEvent() {
        final LocalDate oldExpectedDateReady = LocalDate.now().plusDays(1);
        final LocalDate newExpectedDateReady = LocalDate.now().plusDays(2);

        caseAggregateState.setCaseReceived(true);
        caseAggregateState.setExpectedDateReady(oldExpectedDateReady);

        final CaseExpectedDateReadyChanged caseExpectedDateReadyChanged = new CaseExpectedDateReadyChanged(caseId, oldExpectedDateReady, newExpectedDateReady);

        compositeCaseAggregateStateMutator.apply(caseExpectedDateReadyChanged, caseAggregateState);

        assertFalse(caseAggregateState.isCaseReadyForDecision());
        assertThat(caseAggregateState.getExpectedDateReady(), is(newExpectedDateReady));
    }

    @Test
    public void shouldMutateStateOnCaseReferredForCourtHearingEvent() {
        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing().build();

        compositeCaseAggregateStateMutator.apply(caseReferredForCourtHearing, caseAggregateState);

        assertTrue(caseAggregateState.isCaseReferredForCourtHearing());
    }

    @Test
    public void shouldMutateStateOnCaseReopenedUpdatedEvent() {
        final LocalDate caseReopenedDate = LocalDate.now();
        final CaseReopenedUpdated caseReopenedUpdated = new CaseReopenedUpdated(new CaseReopenDetails(caseId, caseReopenedDate, "", ""));

        compositeCaseAggregateStateMutator.apply(caseReopenedUpdated, caseAggregateState);

        assertThat(caseAggregateState.getCaseReopenedDate(), is(caseReopenedDate));
    }

    @Test
    public void shouldMutateStateOnCaseStartedEvent() {
        final CaseStarted caseStarted = new CaseStarted(caseId);

        compositeCaseAggregateStateMutator.apply(caseStarted, caseAggregateState);

        assertThat(caseAggregateState.getCaseId(), is(caseId));
    }

    @Test
    public void shouldMutateStateOnCaseCompletedEvent() {
        final CaseCompleted caseCompleted = new CaseCompleted(caseId);

        compositeCaseAggregateStateMutator.apply(caseCompleted, caseAggregateState);

        assertTrue(caseAggregateState.isCaseCompleted());
    }

    @Test
    public void shouldMutateStateOnDatesToAvoidAddedEvent() {
        final String datesToAvoid = "datesToAvoid";
        final DatesToAvoidAdded datesToAvoidAdded = new DatesToAvoidAdded(caseId, datesToAvoid);

        compositeCaseAggregateStateMutator.apply(datesToAvoidAdded, caseAggregateState);

        assertThat(caseAggregateState.getDatesToAvoid(), is(datesToAvoid));
    }

    @Test
    public void shouldMutateStateOnDatesToAvoidUpdatedEvent() {
        final String datesToAvoid = "datesToAvoid";
        final DatesToAvoidUpdated datesToAvoidUpdated = new DatesToAvoidUpdated(caseId, datesToAvoid);

        compositeCaseAggregateStateMutator.apply(datesToAvoidUpdated, caseAggregateState);

        assertThat(caseAggregateState.getDatesToAvoid(), is(datesToAvoid));
    }

    @Test
    public void shouldMutateStateOnEmploymentStatusUpdatedEvent() {
        final String employmentStatus = "unemployed";
        final EmploymentStatusUpdated employmentStatusUpdated = new EmploymentStatusUpdated(defendantId, employmentStatus);

        compositeCaseAggregateStateMutator.apply(employmentStatusUpdated, caseAggregateState);

        assertThat(caseAggregateState.getEmploymentStatusByDefendantId().entrySet(), iterableWithSize(1));
    }

    @Test
    public void shouldMutateStateOnEmployerDeletedEvent() {
        caseAggregateState.updateEmploymentStatusForDefendant(defendantId, "employed");

        final EmployerDeleted employerDeleted = new EmployerDeleted(defendantId);

        compositeCaseAggregateStateMutator.apply(employerDeleted, caseAggregateState);

        assertFalse(caseAggregateState.getDefendantEmploymentStatus(defendantId).isPresent());
    }

    @Test
    public void shouldMutateStateOnFinancialMeansUpdatedEvent() {
        caseAggregateState.updateEmploymentStatusForDefendant(defendantId, "employed");

        final String employmentStatus = "employmentStatus";

        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEventForOnlinePlea(
                defendantId,
                new Income(IncomeFrequency.MONTHLY, BigDecimal.TEN),
                new Benefits(),
                employmentStatus,
                new ArrayList<>(),
                now());

        compositeCaseAggregateStateMutator.apply(financialMeansUpdated, caseAggregateState);

        assertThat(caseAggregateState.getDefendantEmploymentStatus(defendantId).get(), is(employmentStatus));
    }

    @Test
    public void shouldMutateStateOnHearingLanguagePreferenceUpdatedForDefendant() {
        final HearingLanguagePreferenceUpdatedForDefendant hearingLanguagePreferenceUpdatedForDefendant =
                HearingLanguagePreferenceUpdatedForDefendant.createEvent(
                        caseId,
                        defendantId,
                        true);

        compositeCaseAggregateStateMutator.apply(hearingLanguagePreferenceUpdatedForDefendant, caseAggregateState);

        assertTrue(caseAggregateState.getDefendantsSpeakWelsh().get(defendantId));
    }

    @Test
    public void shouldMutateStateOnHearingLanguagePreferenceCancelledForDefendant() {
        final HearingLanguagePreferenceCancelledForDefendant event =
                new HearingLanguagePreferenceCancelledForDefendant(
                        caseId,
                        defendantId);

        compositeCaseAggregateStateMutator.apply(event, caseAggregateState);

        assertNull(caseAggregateState.getDefendantsSpeakWelsh().get(defendantId));
    }

    @Test
    public void shouldMutateStateOnInterpreterUpdatedForDefendant() {
        final String interpreterLanguage = "welsh";

        final InterpreterUpdatedForDefendant event = InterpreterUpdatedForDefendant.createEvent(
                caseId,
                defendantId,
                interpreterLanguage);

        compositeCaseAggregateStateMutator.apply(event, caseAggregateState);

        assertThat(caseAggregateState.getDefendantInterpreterLanguage(defendantId), is(interpreterLanguage));
    }

    @Test
    public void shouldMutateStateOnInterpreterCancelledForDefendant() {
        caseAggregateState.updateDefendantInterpreterLanguage(defendantId, Interpreter.of("welsh"));

        final InterpreterCancelledForDefendant event = new InterpreterCancelledForDefendant(
                caseId,
                defendantId);

        compositeCaseAggregateStateMutator.apply(event, caseAggregateState);

        assertNull(caseAggregateState.getDefendantInterpreterLanguage(defendantId));
    }

    @Test
    public void shouldMutateStateOnPleaUpdatedEvent() {
        caseAggregateState.setCaseReceived(true);

        final PleaUpdated pleaUpdated = new PleaUpdated(caseId, offenceId, PleaType.GUILTY, PleaMethod.ONLINE, ZonedDateTime.now());
        compositeCaseAggregateStateMutator.apply(pleaUpdated, caseAggregateState);

        assertTrue(caseAggregateState.getOffenceIdsWithPleas().contains(offenceId));
    }

    @Test
    public void shouldMutateStateOnPleaCancelledEvent() {
        caseAggregateState.setCaseReceived(true);

        final PleaCancelled pleaCancelled = new PleaCancelled(caseId, offenceId);
        compositeCaseAggregateStateMutator.apply(pleaCancelled, caseAggregateState);

        assertFalse(caseAggregateState.getOffenceIdsWithPleas().contains(offenceId));
    }

    @Test
    public void shouldMutateStateOnTrialRequestCancelledEvent() {
        final TrialRequestCancelled trialRequestCancelled = new TrialRequestCancelled(randomUUID());
        compositeCaseAggregateStateMutator.apply(trialRequestCancelled, caseAggregateState);

        assertFalse(caseAggregateState.isTrialRequested());
    }
}
