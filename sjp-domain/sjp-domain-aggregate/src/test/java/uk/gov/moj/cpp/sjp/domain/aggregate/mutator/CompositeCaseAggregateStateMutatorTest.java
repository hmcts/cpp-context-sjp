package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.moj.cpp.sjp.domain.Priority.MEDIUM;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.testutils.builders.DischargeBuilder.withDefaults;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded.caseReferralForCourtHearingRejectionRecorded;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;
import static java.util.Collections.EMPTY_LIST;
import static java.math.BigDecimal.valueOf;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2.caseReferredForCourtHearingV2;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.FinancialImpositionExportDetails;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.SessionCourt;
import uk.gov.moj.cpp.sjp.domain.decision.ConvictingInformation;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.resubmit.PaymentTermsInfo;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.AdjournBuilder;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseEligibleForAOCP;
import uk.gov.moj.cpp.sjp.event.CaseExpectedDateReadyChanged;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUpdated;
import uk.gov.moj.cpp.sjp.event.CaseStarted;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidAdded;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantAcceptedAocp;
import uk.gov.moj.cpp.sjp.event.DefendantAocpResponseTimerExpired;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAdded;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionCorrelationIdAdded;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsStarted;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestReasonChanged;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.VerdictCancelled;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.decision.ConvictionCourtResolved;
import uk.gov.moj.cpp.sjp.event.decision.DecisionResubmitted;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.decommissioned.CaseAssignmentDeleted;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.Test;

public class CompositeCaseAggregateStateMutatorTest {

    private final UUID caseId = randomUUID();
    private final String urn = "TFL12345678";
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
        assertThat(caseAggregateState.isManagedByAtcm(), is(false));
    }

    @Test
    public void shouldMutateStateOnCaseReferralForCourtHearingRejectionRecordedEvent() {
        final CaseReferralForCourtHearingRejectionRecorded event = caseReferralForCourtHearingRejectionRecorded().build();
        compositeCaseAggregateStateMutator.apply(event, caseAggregateState);
        assertThat(caseAggregateState.isManagedByAtcm(), is(true));
    }

    @Test
    public void shouldMutateStateOnCaseReferredForCourtHearingV2Event() {
        final CaseReferredForCourtHearingV2 caseReferredForCourtHearingV2 = caseReferredForCourtHearingV2().build();

        compositeCaseAggregateStateMutator.apply(caseReferredForCourtHearingV2, caseAggregateState);

        assertTrue(caseAggregateState.isCaseReferredForCourtHearing());
        assertThat(caseAggregateState.isManagedByAtcm(), is(false));
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
        final UUID sessionId = randomUUID();
        final CaseCompleted caseCompleted = new CaseCompleted(caseId, Sets.newHashSet(sessionId));

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
                now(), BigDecimal.TEN, BigDecimal.ONE, new Integer(10), false);

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
        final UUID sessionId = randomUUID();
        final DecisionSaved decisionSaved = new DecisionSaved(randomUUID(), sessionId, caseId, urn, now(), offenceDecisions);

        compositeCaseAggregateStateMutator.apply(decisionSaved, caseAggregateState);

        assertThat(caseAggregateState.getOffenceDecisions(), containsInAnyOrder(offence1Decision, offence2Decision));

        assertThat(caseAggregateState.getSessionIds(), hasSize(1));
        assertThat(caseAggregateState.getSessionIds(), hasItem(sessionId));
    }

    @Test
    public void shouldMutateOnDecisionResubmittedEvent() {
        final Withdraw offence1Decision = new Withdraw(randomUUID(), createOffenceDecisionInformation(randomUUID(), VerdictType.NO_VERDICT), randomUUID());
        final Withdraw offence2Decision = new Withdraw(randomUUID(), createOffenceDecisionInformation(randomUUID(), VerdictType.NO_VERDICT), randomUUID());
        final List<OffenceDecision> offenceDecisions = newArrayList(offence1Decision, offence2Decision);
        final UUID sessionId = randomUUID();
        final DecisionSaved decisionSaved = new DecisionSaved(randomUUID(), sessionId, caseId, urn, now(), offenceDecisions);
        final DecisionResubmitted decisionResubmitted = new DecisionResubmitted(decisionSaved, ZonedDateTime.now(),
                new PaymentTermsInfo(10, false), "fixed","SW13213141");

        compositeCaseAggregateStateMutator.apply(decisionResubmitted, caseAggregateState);

        assertThat(caseAggregateState.isDecisionResubmitted(), is(true));

    }

    @Test
    public void shouldMutateOnConvictionCourtResolvedEvent() {

        final SessionCourt convictingCourt = new SessionCourt("1234","001");
        final UUID sessionId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final ConvictingInformation convictingInformation = new ConvictingInformation(ZonedDateTime.now(),convictingCourt,sessionId, offenceId2);
        final List<ConvictingInformation> convictingInformationList = newArrayList(convictingInformation);

        final ConvictionCourtResolved convictionCourtResolved = new ConvictionCourtResolved(caseId,convictingInformationList);

        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(offenceId1, FOUND_GUILTY)
                .build();
        caseAggregateState.updateOffenceConvictionDetails(now(), asList(adjourn), sessionId);
        assertThat(caseAggregateState.getOffencesWithConviction(), not(empty()));

        compositeCaseAggregateStateMutator.apply(convictionCourtResolved, caseAggregateState);

        assertTrue(caseAggregateState.getOffencesWithConviction().contains(offenceId1));
        assertTrue(caseAggregateState.getOffencesWithConviction().contains(offenceId2));
        assertThat(caseAggregateState.getOffencesWithConviction(),hasSize(2));
    }

    @Test
    public void shouldMutateOnConvictionCourtResolvedEventToUpdateOffenceDetail() {

        final SessionCourt convictingCourt = new SessionCourt("1234","001");
        final UUID sessionId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        //Initially offence is adjourned
        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(offenceId1, FOUND_GUILTY)
                .build();

        caseAggregateState.updateOffenceConvictionDetails(now(), asList(adjourn), sessionId);
        assertThat(caseAggregateState.getOffencesWithConviction(), not(empty()));

        //for same offence now convicted
        final ConvictingInformation convictingInformation = new ConvictingInformation(ZonedDateTime.now(),convictingCourt,sessionId, offenceId1);
        final List<ConvictingInformation> convictingInformationList = newArrayList(convictingInformation);

        final ConvictionCourtResolved convictionCourtResolved = new ConvictionCourtResolved(caseId,convictingInformationList);
        compositeCaseAggregateStateMutator.apply(convictionCourtResolved, caseAggregateState);
        assertTrue(caseAggregateState.getOffencesWithConviction().contains(offenceId1));
        assertThat(caseAggregateState.getOffencesWithConviction(),hasSize(1));
    }


    @Test
    public void shouldMutateStateOnDeleteDocsStarted() {
        final FinancialMeansDeleteDocsStarted deleteDocsStarted = new FinancialMeansDeleteDocsStarted(caseId, defendantId);
        compositeCaseAggregateStateMutator.apply(deleteDocsStarted, caseAggregateState);
        assertThat(caseAggregateState.isDeleteDocsStarted(), is(true));
    }

    @Test
    public void shouldMutateStateOnVerdictCancelled() {
        final Discharge discharge = withDefaults()
                .offenceDecisionInformation(createOffenceDecisionInformation(offenceId, FOUND_GUILTY))
                .build();
        caseAggregateState.updateOffenceConvictionDetails(now(), asList(discharge), null);
        assertThat(caseAggregateState.getOffencesWithConviction(), not(empty()));

        final VerdictCancelled verdictCancelled = new VerdictCancelled(offenceId);
        compositeCaseAggregateStateMutator.apply(verdictCancelled, caseAggregateState);
        assertThat(caseAggregateState.getOffencesWithConviction(), empty());
    }

    @Test
    public void shouldMutateStateOnApplicationDecisionSetAside() {
        caseAggregateState.setSetAside(false);
        caseAggregateState.markCaseCompleted();

        final ApplicationDecisionSetAside applicationSetAside = new ApplicationDecisionSetAside(randomUUID(), caseId);
        compositeCaseAggregateStateMutator.apply(applicationSetAside, caseAggregateState);
        assertThat(caseAggregateState.isSetAside(), is(true));
        assertThat(caseAggregateState.isCaseCompleted(), is(false));
    }

    @Test
    public void shouldMutateStateOnApplicationStatusChanged() {
        caseAggregateState.setSetAside(false);
        final UUID applicationId = randomUUID();
        final Application application = new Application(null);
        application.setType(STAT_DEC);
        application.setStatus(STATUTORY_DECLARATION_PENDING);
        caseAggregateState.setCurrentApplication(application);

        final ApplicationStatusChanged applicationStatusChanged = new ApplicationStatusChanged(
                applicationId,
                STATUTORY_DECLARATION_GRANTED);

        compositeCaseAggregateStateMutator.apply(applicationStatusChanged, caseAggregateState);
        assertThat(caseAggregateState.getCurrentApplication().getStatus(), is(STATUTORY_DECLARATION_GRANTED));
    }

    @Test
    public void shouldMutateStateOnFinancialImpositionCorrelationIdAdded() {
        final FinancialImpositionCorrelationIdAdded correlationIdAdded =
                new FinancialImpositionCorrelationIdAdded(caseId, defendantId, randomUUID());

        compositeCaseAggregateStateMutator.apply(correlationIdAdded, caseAggregateState);
        assertThat(caseAggregateState.getDefendantFinancialImpositionExportDetails().size(), is(1));
        assertThat(caseAggregateState.getDefendantFinancialImpositionExportDetails().values(), hasItem(allOf(
                hasProperty("correlationId", is(correlationIdAdded.getCorrelationId()))
        )));
        assertThat(caseAggregateState.isCorrelationIdAllreadyGenerated(), is(true));

    }

    @Test
    public void shouldMutateStateOnFinancialImpositionAccountNumberAdded() {
        final UUID correlationId = randomUUID();
        final String accountNumber = "123456780";

        final FinancialImpositionExportDetails exportDetails = new FinancialImpositionExportDetails();
        exportDetails.setCorrelationId(correlationId);
        caseAggregateState.addFinancialImpositionExportDetails(defendantId, exportDetails);

        final FinancialImpositionAccountNumberAdded accountNumberAdded =
                new FinancialImpositionAccountNumberAdded(caseId, defendantId, accountNumber);

        compositeCaseAggregateStateMutator.apply(accountNumberAdded, caseAggregateState);
        assertThat(caseAggregateState.getDefendantFinancialImpositionExportDetails().size(), is(1));
        assertThat(caseAggregateState.getDefendantFinancialImpositionExportDetails().values(), hasItem(allOf(
                hasProperty("correlationId", is(correlationId)),
                hasProperty("accountNumber", is(accountNumber))
        )));
    }

    @Test
    public void shouldMutateOnDefendantAcceptedAocp() {
        final DefendantAcceptedAocp defendantAcceptedAocp = new DefendantAcceptedAocp(caseId, defendantId, EMPTY_LIST, PleaMethod.ONLINE, null, true, now(), null);
        compositeCaseAggregateStateMutator.apply(defendantAcceptedAocp, caseAggregateState);
        assertThat(caseAggregateState.isDefendantAcceptedAocp(), is(true));
    }

    @Test
    public void shouldMutateOnDefendantAocpResponseTimerExpired() {
        final DefendantAocpResponseTimerExpired defendantAocpResponseTimerExpired = new DefendantAocpResponseTimerExpired(caseId);
        compositeCaseAggregateStateMutator.apply(defendantAocpResponseTimerExpired, caseAggregateState);
        assertThat(caseAggregateState.isAocpAcceptanceResponseTimerExpired(), is(true));
    }

    @Test
    public void shouldMutateOnCaseEligibleForAOCP() {
        final CaseEligibleForAOCP caseEligibleForAOCP = new CaseEligibleForAOCP(caseId, valueOf(5), valueOf(34), valueOf(137), null);
        compositeCaseAggregateStateMutator.apply(caseEligibleForAOCP, caseAggregateState);
        assertThat(caseAggregateState.isAocpEligible(), is(true));
        assertThat(caseAggregateState.getAocpVictimSurcharge(), is(valueOf(34)));
        assertThat(caseAggregateState.getAocpTotalCost(), is(valueOf(137)));
        assertThat(caseAggregateState.isAocpEligible(), is(true));
    }

    @Test
    public void shouldMutateOnPleasSet() {
        final PleasSet pleasSet = new PleasSet(caseId, null, EMPTY_LIST);
        compositeCaseAggregateStateMutator.apply(pleasSet, caseAggregateState);
        assertThat(caseAggregateState.isDefendantAcceptedAocp(), is(false));
    }

}
