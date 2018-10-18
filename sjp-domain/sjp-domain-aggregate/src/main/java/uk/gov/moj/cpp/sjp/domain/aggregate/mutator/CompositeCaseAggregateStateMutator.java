package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import com.google.common.collect.ImmutableMap;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.*;
import uk.gov.moj.cpp.sjp.event.decommissioned.CaseAssignmentDeleted;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.util.Map;
import java.util.Optional;

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
            (event, state) -> state.addOffenceIdWithPleas(event.getOffenceId());
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
    private static final AggregateStateMutator<CaseMarkedReadyForDecision, CaseAggregateState> CASE_READY_MUTATOR =
            (event, state) -> state.setReadinessReason(event.getReason());
    private static final AggregateStateMutator<CaseUnmarkedReadyForDecision, CaseAggregateState> CASE_UNMARKED_READY_MUTATOR =
            (event, state) -> state.setReadinessReason(null);

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
                .put(CaseMarkedReadyForDecision.class, CASE_READY_MUTATOR)
                .put(CaseUnmarkedReadyForDecision.class, CASE_UNMARKED_READY_MUTATOR)
                .put(CaseReopenedUpdated.class, CASE_REOPENED_UPDATED_MUTATOR)
                .put(CaseReceived.class, CaseReceivedMutator.INSTANCE)
                .put(CaseDocumentAdded.class, CaseDocumentAddedMutator.INSTANCE)
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
