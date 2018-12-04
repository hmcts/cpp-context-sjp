package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import static java.time.ZonedDateTime.now;
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
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
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

    @Test
    public void shouldMutateStateOnAllOffencesWithdrawalRequestedEvent() {
        AllOffencesWithdrawalRequested allOffencesWithdrawalRequested = new AllOffencesWithdrawalRequested(UUID.randomUUID());

        CaseAggregateState caseAggregateState = new CaseAggregateState();
        CompositeCaseAggregateStateMutator.INSTANCE.apply(allOffencesWithdrawalRequested, caseAggregateState);

        assertTrue(caseAggregateState.isWithdrawalAllOffencesRequested());
    }

    @Test
    public void shouldMutateStateOnAllOffencesWithdrawalRequestCancelledEvent() {
        AllOffencesWithdrawalRequestCancelled allOffencesWithdrawalRequestCancelled = new AllOffencesWithdrawalRequestCancelled(UUID.randomUUID());

        CaseAggregateState caseAggregateState = new CaseAggregateState();
        CompositeCaseAggregateStateMutator.INSTANCE.apply(allOffencesWithdrawalRequestCancelled, caseAggregateState);

        assertFalse(caseAggregateState.isWithdrawalAllOffencesRequested());
    }

    @Test
    public void shouldMutateStateOnCaseAssignedEvent() {
        UUID userId = UUID.randomUUID();
        CaseAssigned caseAssigned = new CaseAssigned(UUID.randomUUID(), userId, now(), CaseAssignmentType.MAGISTRATE_DECISION);

        CaseAggregateState caseAggregateState = new CaseAggregateState();
        CompositeCaseAggregateStateMutator.INSTANCE.apply(caseAssigned, caseAggregateState);

        assertTrue(caseAggregateState.isAssignee(userId));
    }

    @Test
    public void shouldMutateStateOnCaseUnassignedEvent() {
        UUID userId = UUID.randomUUID();

        CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setAssigneeId(userId);

        CaseUnassigned caseUnassigned = new CaseUnassigned(UUID.randomUUID());

        CompositeCaseAggregateStateMutator.INSTANCE.apply(caseUnassigned, caseAggregateState);

        assertFalse(caseAggregateState.isAssignee(userId));
    }

    @Test
    public void shouldMutateStateOnCaseAssignmentDeletedEvent() {
        UUID userId = UUID.randomUUID();

        CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setAssigneeId(userId);

        CaseAssignmentDeleted caseAssignmentDeleted = new CaseAssignmentDeleted(UUID.randomUUID(), CaseAssignmentType.MAGISTRATE_DECISION);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(caseAssignmentDeleted, caseAggregateState);

        assertFalse(caseAggregateState.isAssignee(userId));
    }

    @Test
    public void shouldMutateStateOnCaseMarkedReadyForDecisionEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseReceived(true);

        CaseReadinessReason readinessReason = CaseReadinessReason.PLEADED_GUILTY;
        CaseMarkedReadyForDecision caseMarkedReadyForDecision = new CaseMarkedReadyForDecision(UUID.randomUUID(), readinessReason, now());

        CompositeCaseAggregateStateMutator.INSTANCE.apply(caseMarkedReadyForDecision, caseAggregateState);

        assertThat(caseAggregateState.getReadinessReason(), is(readinessReason));
    }

    @Test
    public void shouldMutateStateOnCaseUnmarkedReadyForDecisionEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseReceived(true);

        CaseUnmarkedReadyForDecision caseUnmarkedReadyForDecision = new CaseUnmarkedReadyForDecision(UUID.randomUUID());

        CompositeCaseAggregateStateMutator.INSTANCE.apply(caseUnmarkedReadyForDecision, caseAggregateState);

        assertNull(caseAggregateState.getReadinessReason());
    }

    @Test
    public void shouldMutateStateOnCaseReferredForCourtHearingEvent() {
        CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing().build();

        CaseAggregateState caseAggregateState = new CaseAggregateState();
        CompositeCaseAggregateStateMutator.INSTANCE.apply(caseReferredForCourtHearing, caseAggregateState);

        assertTrue(caseAggregateState.isCaseReferredForCourtHearing());
    }

    @Test
    public void shouldMutateStateOnCaseReopenedUpdatedEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();

        LocalDate caseReopenedDate = LocalDate.now();
        CaseReopenedUpdated caseReopenedUpdated = new CaseReopenedUpdated(new CaseReopenDetails(UUID.randomUUID(), caseReopenedDate, "", ""));

        CompositeCaseAggregateStateMutator.INSTANCE.apply(caseReopenedUpdated, caseAggregateState);

        assertThat(caseAggregateState.getCaseReopenedDate(), is(caseReopenedDate));
    }

    @Test
    public void shouldMutateStateOnCaseStartedEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();

        UUID caseId = UUID.randomUUID();
        CaseStarted caseStarted = new CaseStarted(caseId);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(caseStarted, caseAggregateState);

        assertThat(caseAggregateState.getCaseId(), is(caseId));
    }

    @Test
    public void shouldMutateStateOnCaseCompletedEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();

        UUID caseId = UUID.randomUUID();
        CaseCompleted caseCompleted = new CaseCompleted(caseId);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(caseCompleted, caseAggregateState);

        assertTrue(caseAggregateState.isCaseCompleted());
    }

    @Test
    public void shouldMutateStateOnDatesToAvoidAddedEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();

        UUID caseId = UUID.randomUUID();
        String datesToAvoid = "datesToAvoid";
        DatesToAvoidAdded datesToAvoidAdded = new DatesToAvoidAdded(caseId, datesToAvoid);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(datesToAvoidAdded, caseAggregateState);

        assertThat(caseAggregateState.getDatesToAvoid(), is(datesToAvoid));
    }

    @Test
    public void shouldMutateStateOnDatesToAvoidUpdatedEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();

        UUID caseId = UUID.randomUUID();
        String datesToAvoid = "datesToAvoid";
        DatesToAvoidUpdated datesToAvoidUpdated = new DatesToAvoidUpdated(caseId, datesToAvoid);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(datesToAvoidUpdated, caseAggregateState);

        assertThat(caseAggregateState.getDatesToAvoid(), is(datesToAvoid));
    }

    @Test
    public void shouldMutateStateOnEmploymentStatusUpdatedEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();

        UUID defendantId = UUID.randomUUID();
        String employmentStatus = "unemployed";
        EmploymentStatusUpdated employmentStatusUpdated = new EmploymentStatusUpdated(defendantId, employmentStatus);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(employmentStatusUpdated, caseAggregateState);

        assertThat(caseAggregateState.getEmploymentStatusByDefendantId().entrySet(), iterableWithSize(1));
    }

    @Test
    public void shouldMutateStateOnEmployerDeletedEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();
        UUID defendantId = UUID.randomUUID();

        caseAggregateState.updateEmploymentStatusForDefendant(defendantId, "employed");

        EmployerDeleted employerDeleted = new EmployerDeleted(defendantId);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(employerDeleted, caseAggregateState);

        assertFalse(caseAggregateState.getDefendantEmploymentStatus(defendantId).isPresent());
    }

    @Test
    public void shouldMutateStateOnFinancialMeansUpdatedEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();
        UUID defendantId = UUID.randomUUID();

        caseAggregateState.updateEmploymentStatusForDefendant(defendantId, "employed");

        String employmentStatus = "employmentStatus";

        FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEventForOnlinePlea(
                defendantId,
                new Income(IncomeFrequency.MONTHLY, BigDecimal.TEN),
                new Benefits(),
                employmentStatus,
                new ArrayList<>(),
                now());

        CompositeCaseAggregateStateMutator.INSTANCE.apply(financialMeansUpdated, caseAggregateState);

        assertThat(caseAggregateState.getDefendantEmploymentStatus(defendantId).get(), is(employmentStatus));
    }

    @Test
    public void shouldMutateStateOnHearingLanguagePreferenceUpdatedForDefendant() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();
        UUID caseId = UUID.randomUUID();

        UUID defendantId = UUID.randomUUID();
        HearingLanguagePreferenceUpdatedForDefendant hearingLanguagePreferenceUpdatedForDefendant =
                HearingLanguagePreferenceUpdatedForDefendant.createEvent(
                        caseId,
                        defendantId,
                        true);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(hearingLanguagePreferenceUpdatedForDefendant, caseAggregateState);

        assertTrue(caseAggregateState.getDefendantsSpeakWelsh().get(defendantId));
    }

    @Test
    public void shouldMutateStateOnHearingLanguagePreferenceCancelledForDefendant() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();
        UUID caseId = UUID.randomUUID();

        UUID defendantId = UUID.randomUUID();
        HearingLanguagePreferenceCancelledForDefendant event =
                new HearingLanguagePreferenceCancelledForDefendant(
                        caseId,
                        defendantId);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(event, caseAggregateState);

        assertNull(caseAggregateState.getDefendantsSpeakWelsh().get(defendantId));
    }

    @Test
    public void shouldMutateStateOnInterpreterUpdatedForDefendant() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();

        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        String interpreterLanguage = "welsh";

        InterpreterUpdatedForDefendant event = InterpreterUpdatedForDefendant.createEvent(
                caseId,
                defendantId,
                interpreterLanguage);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(event, caseAggregateState);

        assertThat(caseAggregateState.getDefendantInterpreterLanguage(defendantId), is(interpreterLanguage));
    }

    @Test
    public void shouldMutateStateOnInterpreterCancelledForDefendant() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();
        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();

        caseAggregateState.updateDefendantInterpreterLanguage(defendantId, Interpreter.of("welsh"));

        InterpreterCancelledForDefendant event = new InterpreterCancelledForDefendant(
                caseId,
                defendantId);

        CompositeCaseAggregateStateMutator.INSTANCE.apply(event, caseAggregateState);

        assertNull(caseAggregateState.getDefendantInterpreterLanguage(defendantId));
    }

    @Test
    public void shouldMutateStateOnPleaUpdatedEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseReceived(true);

        UUID offenceId = UUID.randomUUID();
        PleaUpdated pleaUpdated = new PleaUpdated(UUID.randomUUID(), offenceId, PleaType.GUILTY, PleaMethod.ONLINE, ZonedDateTime.now());
        CompositeCaseAggregateStateMutator.INSTANCE.apply(pleaUpdated, caseAggregateState);

        assertTrue(caseAggregateState.getOffenceIdsWithPleas().contains(offenceId));
    }

    @Test
    public void shouldMutateStateOnPleaCancelledEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseReceived(true);

        UUID offenceId = UUID.randomUUID();
        PleaCancelled pleaCancelled = new PleaCancelled(UUID.randomUUID(), offenceId);
        CompositeCaseAggregateStateMutator.INSTANCE.apply(pleaCancelled, caseAggregateState);

        assertFalse(caseAggregateState.getOffenceIdsWithPleas().contains(offenceId));
    }

    @Test
    public void shouldMutateStateOnTrialRequestCancelledEvent() {
        CaseAggregateState caseAggregateState = new CaseAggregateState();

        TrialRequestCancelled trialRequestCancelled = new TrialRequestCancelled(UUID.randomUUID());
        CompositeCaseAggregateStateMutator.INSTANCE.apply(trialRequestCancelled, caseAggregateState);

        assertFalse(caseAggregateState.isTrialRequested());
    }
}
