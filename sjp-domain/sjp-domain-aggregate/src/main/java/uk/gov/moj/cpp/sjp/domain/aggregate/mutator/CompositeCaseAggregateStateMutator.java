package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseExpectedDateReadyChanged;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUndone;
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
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.TrialRequested;
import uk.gov.moj.cpp.sjp.event.decommissioned.CaseAssignmentDeleted;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

/**
 * Defines the composite {@link AggregateStateMutator} which delegates to the appropriate mutator
 * based on the event handled.
 */
final class CompositeCaseAggregateStateMutator implements AggregateStateMutator<Object, CaseAggregateState> {

    private static final AggregateStateMutator<CaseStarted, CaseAggregateState> CASE_STARTED_MUTATOR =
            (event, state) -> state.setCaseId(event.getId());
    private static final AggregateStateMutator<DatesToAvoidAdded, CaseAggregateState> DATES_TO_AVOID_MUTATOR =
            (event, state) -> state.setDatesToAvoid(event.getDatesToAvoid());
    private static final AggregateStateMutator<DatesToAvoidUpdated, CaseAggregateState> DATES_TO_AVOID_UPDATED_MUTATOR =
            (event, state) -> state.setDatesToAvoid(event.getDatesToAvoid());
    private static final AggregateStateMutator<CaseCompleted, CaseAggregateState> CASE_COMPLETED_MUTATOR =
            (event, state) -> state.markCaseCompleted();
    private static final AggregateStateMutator<PleaUpdated, CaseAggregateState> PLEA_UPDATED_MUTATOR =
            (event, state) -> state.updateOffenceWithPlea(event.getOffenceId());
    private static final AggregateStateMutator<PleaCancelled, CaseAggregateState> PLEA_CANCELLED_MUTATOR =
            (event, state) -> state.removePleaFromOffence(event.getOffenceId());
    private static final AggregateStateMutator<TrialRequestCancelled, CaseAggregateState> TRIAL_REQUEST_CANCELLED_MUTATOR =
            (event, state) -> state.setTrialRequested(false);
    private static final AggregateStateMutator<InterpreterUpdatedForDefendant, CaseAggregateState> UPDATE_INTERPRETER_MUTATOR =
            (event, state) -> state.updateDefendantInterpreterLanguage(event.getDefendantId(), event.getInterpreter());
    private static final AggregateStateMutator<HearingLanguagePreferenceUpdatedForDefendant, CaseAggregateState> HEARING_LANGUAGE_PREFERENCE_MUTATOR =
            (event, state) -> state.updateDefendantSpeakWelsh(event.getDefendantId(), event.getSpeakWelsh());
    private static final AggregateStateMutator<HearingLanguagePreferenceCancelledForDefendant, CaseAggregateState> HEARING_LANGUAGE_PREFERENCE_CANCELLED_MUTATOR =
            (event, state) -> state.removeDefendantSpeakWelshPreference(event.getDefendantId());
    private static final AggregateStateMutator<InterpreterCancelledForDefendant, CaseAggregateState> INTERPRETER_CANCELLED_MUTATOR =
            (event, state) -> state.removeInterpreterForDefendant(event.getDefendantId());
    private static final AggregateStateMutator<AllOffencesWithdrawalRequested, CaseAggregateState> ALL_OFFENCES_WITHDRAWAL_MUTATOR =
            (event, state) -> state.setWithdrawalAllOffencesRequested(true);
    private static final AggregateStateMutator<AllOffencesWithdrawalRequestCancelled, CaseAggregateState> ALL_OFFENCES_CANCELLED_MUTATOR =
            (event, state) -> state.setWithdrawalAllOffencesRequested(false);
    private static final AggregateStateMutator<CaseReferredForCourtHearing, CaseAggregateState> CASE_REFERRED_FOR_COURT_HEARING_MUTATOR =
            (event, state) -> state.markCaseReferredForCourtHearing();
    private static final AggregateStateMutator<CaseReopenedUpdated, CaseAggregateState> CASE_REOPENED_UPDATED_MUTATOR =
            (event, state) -> state.setCaseReopenedDate(event.getCaseReopenDetails().getReopenedDate());
    private static final AggregateStateMutator<EmploymentStatusUpdated, CaseAggregateState> DEFENDANT_EMPLOYMENT_STATUS_MUTATOR =
            (event, state) -> state.updateEmploymentStatusForDefendant(event.getDefendantId(), event.getEmploymentStatus());
    private static final AggregateStateMutator<FinancialMeansUpdated, CaseAggregateState> DEFENDANT_FINANCIAL_MEANS_MUTATOR =
            (event, state) -> state.updateEmploymentStatusForDefendant(event.getDefendantId(), event.getEmploymentStatus());
    private static final AggregateStateMutator<EmployerDeleted, CaseAggregateState> EMPLOYER_DELETED_MUTATOR =
            (event, state) -> state.removeEmploymentStatusForDefendant(event.getDefendantId());
    private static final AggregateStateMutator<CaseAssigned, CaseAggregateState> CASE_ASSIGNED_MUTATOR =
            (event, state) -> state.setAssigneeId(event.getAssigneeId());
    private static final AggregateStateMutator<CaseUnassigned, CaseAggregateState> CASE_UNASSIGNED_MUTATOR =
            (event, state) -> state.setAssigneeId(null);
    private static final AggregateStateMutator<CaseAssignmentDeleted, CaseAggregateState> CASE_ASSIGNMENT_DELETED_MUTATOR =
            (event, state) -> state.setAssigneeId(null);
    private static final AggregateStateMutator<CaseExpectedDateReadyChanged, CaseAggregateState> CASE_EXPECTED_DATE_READY_CHAGED_MUTATOR =
            (event, state) -> state.setExpectedDateReady(event.getNewExpectedDateReady());

    static final CompositeCaseAggregateStateMutator INSTANCE = new CompositeCaseAggregateStateMutator();

    private final Map<Class, AggregateStateMutator> eventToStateMutator;

    @SuppressWarnings({"deprecation", "squid:S1602"})
    private CompositeCaseAggregateStateMutator() {
        this.eventToStateMutator = ImmutableMap.<Class, AggregateStateMutator>builder()
                .put(AllOffencesWithdrawalRequested.class, ALL_OFFENCES_WITHDRAWAL_MUTATOR)
                .put(AllOffencesWithdrawalRequestCancelled.class, ALL_OFFENCES_CANCELLED_MUTATOR)
                .put(CaseAssigned.class, CASE_ASSIGNED_MUTATOR)
                .put(CaseUnassigned.class, CASE_UNASSIGNED_MUTATOR)
                .put(CaseAssignmentDeleted.class, CASE_ASSIGNMENT_DELETED_MUTATOR)
                .put(CaseMarkedReadyForDecision.class, CaseMarkedReadyForDecisionMutator.INSTANCE)
                .put(CaseUnmarkedReadyForDecision.class, CaseUnmarkedReadyForDecisionMutator.INSTANCE)
                .put(CaseExpectedDateReadyChanged.class, CASE_EXPECTED_DATE_READY_CHAGED_MUTATOR)
                .put(CaseReopenedUpdated.class, CASE_REOPENED_UPDATED_MUTATOR)
                .put(CaseReceived.class, CaseReceivedMutator.INSTANCE)
                .put(CaseDocumentAdded.class, CaseDocumentAddedMutator.INSTANCE)
                .put(CaseReferredForCourtHearing.class, CASE_REFERRED_FOR_COURT_HEARING_MUTATOR)
                .put(CaseReopenedUndone.class, CaseReopenedUndoneMutator.INSTANCE)
                .put(CaseReopened.class, CaseReopenedMutator.INSTANCE)
                .put(CaseStarted.class, CASE_STARTED_MUTATOR)
                .put(CaseCompleted.class, CASE_COMPLETED_MUTATOR)
                .put(DatesToAvoidAdded.class, DATES_TO_AVOID_MUTATOR)
                .put(DatesToAvoidUpdated.class, DATES_TO_AVOID_UPDATED_MUTATOR)
                .put(DefendantDetailsUpdatedMutator.class, DefendantDetailsUpdatedMutator.INSTANCE)
                .put(EmploymentStatusUpdated.class, DEFENDANT_EMPLOYMENT_STATUS_MUTATOR)
                .put(EmployerDeleted.class, EMPLOYER_DELETED_MUTATOR)
                .put(FinancialMeansUpdated.class, DEFENDANT_FINANCIAL_MEANS_MUTATOR)
                .put(HearingLanguagePreferenceUpdatedForDefendant.class, HEARING_LANGUAGE_PREFERENCE_MUTATOR)
                .put(HearingLanguagePreferenceCancelledForDefendant.class, HEARING_LANGUAGE_PREFERENCE_CANCELLED_MUTATOR)
                .put(InterpreterUpdatedForDefendant.class, UPDATE_INTERPRETER_MUTATOR)
                .put(InterpreterCancelledForDefendant.class, INTERPRETER_CANCELLED_MUTATOR)
                .put(PleaUpdated.class, PLEA_UPDATED_MUTATOR)
                .put(PleaCancelled.class, PLEA_CANCELLED_MUTATOR)
                .put(SjpCaseCreated.class, CaseCreatedMutator.INSTANCE)
                .put(TrialRequestCancelled.class, TRIAL_REQUEST_CANCELLED_MUTATOR)
                .put(TrialRequested.class, TrialRequestedMutator.INSTANCE)
                .build();
    }

    @Override
    public void apply(final Object event, final CaseAggregateState aggregateState) {
        Optional.ofNullable(eventToStateMutator.get(event.getClass()))
                .ifPresent(stateMutator -> stateMutator.apply(event, aggregateState));
    }

}
