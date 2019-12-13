package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.Test;
import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.sjp.domain.*;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.*;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.decommissioned.CaseAssignmentDeleted;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.sjp.domain.Priority.MEDIUM;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

public class CompositeCaseAggregateStateMutatorTest {

    private final UUID caseId = randomUUID();
    private final UUID userId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID offenceId = randomUUID();
    private final UUID oldWithdrawalReasonId = randomUUID();
    private final UUID newWithdrawalReasonId = randomUUID();
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
        final CaseMarkedReadyForDecision caseMarkedReadyForDecision = new CaseMarkedReadyForDecision(caseId, readinessReason, now(), MAGISTRATE, MEDIUM);

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
    public void shouldMutateStateOnEmployerUpdatedEvent() {
        final EmployerUpdated employerUpdated = EmployerUpdated.createEvent(defendantId,
                new Employer(defendantId, "name", "employerReference", "0208123456",
                        new Address("address1", "address2", "address3", "address4", "address5", "BN16HD")));

        compositeCaseAggregateStateMutator.apply(employerUpdated, caseAggregateState);

        assertTrue(caseAggregateState.hasEmployerDetailsUpdated());
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
        final UUID defendantId = UUID.randomUUID();
        caseAggregateState.setCaseReceived(true);
        caseAggregateState.setDefendantId(defendantId);
        final ZonedDateTime dateTime = ZonedDateTime.now();

        final PleaUpdated pleaUpdated = new PleaUpdated(caseId, offenceId, PleaType.GUILTY,
                "mitigation" , "reason" , PleaMethod.ONLINE ,dateTime);
        final Plea plea = new Plea(defendantId, offenceId, PleaType.GUILTY, "reason", "mitigation");
        compositeCaseAggregateStateMutator.apply(pleaUpdated, caseAggregateState);

        assertTrue(caseAggregateState.getOffenceIdsWithPleas().contains(offenceId));

        assertThat(caseAggregateState.getOffencePleaDates().get(offenceId).getDayOfMonth(), is(dateTime.getDayOfMonth()));
        assertThat(caseAggregateState.getOffencePleaDates().get(offenceId).getMonth(), is(dateTime.getMonth()));
        assertThat(caseAggregateState.getOffencePleaDates().get(offenceId).getYear(), is(dateTime.getYear()));

        assertThat(caseAggregateState.getPleas().size(), is(1));
        assertThat(caseAggregateState.getPleas().get(0), is(plea));
    }

    @Test
    public void shouldMutateStateOnPleaCancelledEvent() {
        caseAggregateState.setCaseReceived(true);

        final PleaCancelled pleaCancelled = new PleaCancelled(caseId, offenceId, defendantId);
        compositeCaseAggregateStateMutator.apply(pleaCancelled, caseAggregateState);

        assertFalse(caseAggregateState.getOffenceIdsWithPleas().contains(offenceId));
    }

    @Test
    public void shouldMutateStateOnTrialRequestCancelledEvent() {
        final TrialRequestCancelled trialRequestCancelled = new TrialRequestCancelled(randomUUID());
        compositeCaseAggregateStateMutator.apply(trialRequestCancelled, caseAggregateState);

        assertFalse(caseAggregateState.isTrialRequested());
    }

    @Test
    public void shouldMutateStateOnOffenceWithdrawalRequestCancelled() {
        final OffenceWithdrawalRequested offenceWithdrawalRequested = new OffenceWithdrawalRequested(caseId,
                offenceId,
                oldWithdrawalReasonId,
                randomUUID(),
                ZonedDateTime.now());
        compositeCaseAggregateStateMutator.apply(offenceWithdrawalRequested, caseAggregateState);
        assertThat(caseAggregateState.getWithdrawalRequests(), contains(new WithdrawalRequestsStatus(offenceId, oldWithdrawalReasonId)));

        final OffenceWithdrawalRequestCancelled offenceWithdrawalRequestCancelled = new OffenceWithdrawalRequestCancelled(caseId,
                offenceId,
                randomUUID(),
                ZonedDateTime.now());
        compositeCaseAggregateStateMutator.apply(offenceWithdrawalRequestCancelled, caseAggregateState);
        assertThat(caseAggregateState.getWithdrawalRequests().size(), is(0));
    }

    @Test
    public void shouldMutateStateOnOffenceWithdrawalRequestReasonChanged() {
         final OffenceWithdrawalRequested offenceWithdrawalRequested = new OffenceWithdrawalRequested(caseId,
                 offenceId,
                 oldWithdrawalReasonId,
                 randomUUID(),
                 ZonedDateTime.now());

        compositeCaseAggregateStateMutator.apply(offenceWithdrawalRequested, caseAggregateState);

        assertThat(caseAggregateState.getWithdrawalRequests(), contains(new WithdrawalRequestsStatus(offenceId, oldWithdrawalReasonId)));

        final OffenceWithdrawalRequestReasonChanged offenceWithdrawalRequestReasonChanged = new OffenceWithdrawalRequestReasonChanged(caseId,
                offenceId,
                randomUUID(),
                ZonedDateTime.now(),
                newWithdrawalReasonId,
                oldWithdrawalReasonId);
        CompositeCaseAggregateStateMutator.INSTANCE.apply(offenceWithdrawalRequestReasonChanged, caseAggregateState);
        assertThat(caseAggregateState.getWithdrawalRequests(), contains(new WithdrawalRequestsStatus(offenceId, newWithdrawalReasonId)));
    }

    @Test
    public void shouldMutateOnDecisionSavedEvent() {
        final Withdraw offence1Decision = new Withdraw(randomUUID(), createOffenceDecisionInformation(randomUUID(), VerdictType.NO_VERDICT), randomUUID());
        final Withdraw offence2Decision = new Withdraw(randomUUID(), createOffenceDecisionInformation(randomUUID(), VerdictType.NO_VERDICT), randomUUID());
        final List<OffenceDecision> offenceDecisions = newArrayList(offence1Decision, offence2Decision);
        final DecisionSaved decisionSaved = new DecisionSaved(randomUUID(), randomUUID(), caseId, now(), offenceDecisions);

        compositeCaseAggregateStateMutator.apply(decisionSaved, caseAggregateState);

        assertThat(caseAggregateState.getOffenceDecisions(), containsInAnyOrder(offence1Decision, offence2Decision));
    }
}
