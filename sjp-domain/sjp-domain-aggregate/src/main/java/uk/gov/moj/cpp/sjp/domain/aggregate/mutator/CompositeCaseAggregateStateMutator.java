package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseAdjournmentToLaterSjpHearingElapsed;
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
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidTimerExpired;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantResponseTimerExpired;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleted;
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
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.TrialRequested;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.decommissioned.CaseAssignmentDeleted;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.time.LocalDate;
import java.util.Arrays;
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
            (event, state) -> {
                state.updateOffenceWithPlea(event.getOffenceId());
                state.putOffencePleaDate(event.getOffenceId(),
                        LocalDate.of(
                                event.getUpdatedDate().getYear(),
                                event.getUpdatedDate().getMonth(),
                                event.getUpdatedDate().getDayOfMonth()));
                final Plea plea = new Plea(state.getDefendantId(),
                        event.getOffenceId(),
                        event.getPlea(),
                        event.getNotGuiltyBecause(),
                        event.getMitigation());
                state.setPleas(Arrays.asList(plea));
            };
    private static final AggregateStateMutator<PleaCancelled, CaseAggregateState> PLEA_CANCELLED_MUTATOR =
            (event, state) -> {
                state.removePleaFromOffence(event.getOffenceId());
                state.removeOffencePleaDate(event.getOffenceId());
            };
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
    private static final AggregateStateMutator<FinancialMeansDeleted, CaseAggregateState> DEFENDANT_FINANCIAL_MEANS_DELETED_MUTATOR =
            (event, state) -> state.deleteFinancialMeansData();
    private static final AggregateStateMutator<EmployerDeleted, CaseAggregateState> EMPLOYER_DELETED_MUTATOR =
            (event, state) -> state.removeEmploymentStatusForDefendant(event.getDefendantId());
    private static final AggregateStateMutator<EmployerUpdated, CaseAggregateState> EMPLOYER_UPDATED_MUTATOR =
            (event, state) -> state.setEmployerDetailsUpdated(true);
    private static final AggregateStateMutator<CaseAssigned, CaseAggregateState> CASE_ASSIGNED_MUTATOR =
            (event, state) -> state.setAssigneeId(event.getAssigneeId());
    private static final AggregateStateMutator<CaseUnassigned, CaseAggregateState> CASE_UNASSIGNED_MUTATOR =
            (event, state) -> state.setAssigneeId(null);
    private static final AggregateStateMutator<CaseAssignmentDeleted, CaseAggregateState> CASE_ASSIGNMENT_DELETED_MUTATOR =
            (event, state) -> state.setAssigneeId(null);
    private static final AggregateStateMutator<OffenceWithdrawalRequested, CaseAggregateState> OFFENCE_WITHDRAWAL_REQUESTED_MUTATOR =
            (event, state) -> state.addWithdrawnOffences(new WithdrawalRequestsStatus(event.getOffenceId(), event.getWithdrawalRequestReasonId()));
    private static final AggregateStateMutator<OffenceWithdrawalRequestCancelled, CaseAggregateState> OFFENCE_WITHDRAWAL_REQUESTED_CANCELLED_MUTATOR =
            (event, state) -> state.cancelWithdrawnOffence(event.getOffenceId());
    private static final AggregateStateMutator<OffenceWithdrawalRequestReasonChanged, CaseAggregateState> OFFENCE_WITHDRAWAL_REQUESTED_REASON_CHANGED_MUTATOR =
            (event, state) -> state.updateWithdrawnOffence(new WithdrawalRequestsStatus(event.getOffenceId(), event.getNewWithdrawalRequestReasonId()));
    private static final AggregateStateMutator<CaseExpectedDateReadyChanged, CaseAggregateState> CASE_EXPECTED_DATE_READY_CHANGED_MUTATOR =
            (event, state) -> state.setExpectedDateReady(event.getNewExpectedDateReady());
    private static final AggregateStateMutator<PleadedGuilty, CaseAggregateState> PLEADED_GUILTY_MUTATOR =
            ((event, state) -> {
                state.updateOffenceWithPlea(event.getOffenceId());
                state.putOffencePleaDate(event.getOffenceId(), LocalDate.of(event.getPleadDate().getYear(), event.getPleadDate().getMonth(), event.getPleadDate().getDayOfMonth()));
            });
    private static final AggregateStateMutator<PleadedGuiltyCourtHearingRequested, CaseAggregateState> PLEADED_GUILTY_COURT_HEARING_REQUESTED_MUTATOR =
            ((event, state) -> {
                state.updateOffenceWithPlea(event.getOffenceId());
                state.putOffencePleaDate(event.getOffenceId(), LocalDate.of(event.getPleadDate().getYear(), event.getPleadDate().getMonth(), event.getPleadDate().getDayOfMonth()));
            });
    private static final AggregateStateMutator<PleadedNotGuilty, CaseAggregateState> PLEADED_NOT_GUILTY_MUTATOR =
            ((event, state) -> {
                state.updateOffenceWithPlea(event.getOffenceId());
                state.putOffencePleaDate(event.getOffenceId(), LocalDate.of(event.getPleadDate().getYear(), event.getPleadDate().getMonth(), event.getPleadDate().getDayOfMonth()));
            });
    private static final AggregateStateMutator<DecisionSaved, CaseAggregateState> DECISION_MUTATOR =
            ((event, state) -> state.updateOffenceDecisions(event.getOffenceDecisions(), event.getSessionId()));
    private static final AggregateStateMutator<PleasSet, CaseAggregateState> PLEAS_SET_MUTATOR =
            ((event, state) -> state.setPleas(event.getPleas()));

    private static final AggregateStateMutator<CaseAdjournedToLaterSjpHearingRecorded, CaseAggregateState> CASE_ADJOURNED_TO_LATER_HEARING_RECORDED_MUTATOR =
            ((event, state) -> state.setAdjournedTo(event.getAdjournedTo()));

    private static final AggregateStateMutator<CaseAdjournmentToLaterSjpHearingElapsed, CaseAggregateState> CASE_ADJOURNED_TO_LATER_HEARING_ELAPSED_MUTATOR =
            ((event, state) -> state.makeNonAdjourned());

    private static final AggregateStateMutator<DatesToAvoidTimerExpired, CaseAggregateState> DATES_TO_AVOID_TIMER_EXPIRED_MUTATOR =
            ((event, state) -> state.datesToAvoidTimerExpired());

    private static final AggregateStateMutator<DefendantResponseTimerExpired, CaseAggregateState> DEFENDANT_RESPONSE_TIMER_EXPIRED =
            ((event, state) -> state.setDefendantsResponseTimerExpired());

    private static final AggregateStateMutator<DatesToAvoidRequired, CaseAggregateState> DATES_TO_AVOID_REQUIRED =
            ((event, state) -> {
                state.setDatesToAvoidExpirationDate(event.getDatesToAvoidExpirationDate());
                state.setDatesToAvoidPreviouslyRequested();
            });


    static final CompositeCaseAggregateStateMutator INSTANCE = new CompositeCaseAggregateStateMutator();

    private final Map<Class, AggregateStateMutator> eventToStateMutator;

    @SuppressWarnings({"deprecation", "squid:S1602"})
    private CompositeCaseAggregateStateMutator() {
        this.eventToStateMutator = ImmutableMap.<Class, AggregateStateMutator>builder()
                .put(AllOffencesWithdrawalRequested.class, ALL_OFFENCES_WITHDRAWAL_MUTATOR)
                .put(AllOffencesWithdrawalRequestCancelled.class, ALL_OFFENCES_CANCELLED_MUTATOR)
                .put(CaseAdjournedToLaterSjpHearingRecorded.class, CASE_ADJOURNED_TO_LATER_HEARING_RECORDED_MUTATOR)
                .put(CaseAdjournmentToLaterSjpHearingElapsed.class, CASE_ADJOURNED_TO_LATER_HEARING_ELAPSED_MUTATOR)
                .put(CaseAssigned.class, CASE_ASSIGNED_MUTATOR)
                .put(CaseUnassigned.class, CASE_UNASSIGNED_MUTATOR)
                .put(CaseAssignmentDeleted.class, CASE_ASSIGNMENT_DELETED_MUTATOR)
                .put(CaseMarkedReadyForDecision.class, CaseMarkedReadyForDecisionMutator.INSTANCE)
                .put(CaseUnmarkedReadyForDecision.class, CaseUnmarkedReadyForDecisionMutator.INSTANCE)
                .put(CaseExpectedDateReadyChanged.class, CASE_EXPECTED_DATE_READY_CHANGED_MUTATOR)
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
                .put(EmployerUpdated.class, EMPLOYER_UPDATED_MUTATOR)
                .put(FinancialMeansUpdated.class, DEFENDANT_FINANCIAL_MEANS_MUTATOR)
                .put(FinancialMeansDeleted.class, DEFENDANT_FINANCIAL_MEANS_DELETED_MUTATOR)
                .put(HearingLanguagePreferenceUpdatedForDefendant.class, HEARING_LANGUAGE_PREFERENCE_MUTATOR)
                .put(HearingLanguagePreferenceCancelledForDefendant.class, HEARING_LANGUAGE_PREFERENCE_CANCELLED_MUTATOR)
                .put(InterpreterUpdatedForDefendant.class, UPDATE_INTERPRETER_MUTATOR)
                .put(InterpreterCancelledForDefendant.class, INTERPRETER_CANCELLED_MUTATOR)
                .put(PleaUpdated.class, PLEA_UPDATED_MUTATOR)
                .put(PleaCancelled.class, PLEA_CANCELLED_MUTATOR)
                .put(SjpCaseCreated.class, CaseCreatedMutator.INSTANCE)
                .put(TrialRequestCancelled.class, TRIAL_REQUEST_CANCELLED_MUTATOR)
                .put(TrialRequested.class, TrialRequestedMutator.INSTANCE)
                .put(OffenceWithdrawalRequested.class, OFFENCE_WITHDRAWAL_REQUESTED_MUTATOR)
                .put(OffenceWithdrawalRequestCancelled.class, OFFENCE_WITHDRAWAL_REQUESTED_CANCELLED_MUTATOR)
                .put(OffenceWithdrawalRequestReasonChanged.class, OFFENCE_WITHDRAWAL_REQUESTED_REASON_CHANGED_MUTATOR)
                .put(PleadedGuilty.class, PLEADED_GUILTY_MUTATOR)
                .put(PleadedGuiltyCourtHearingRequested.class, PLEADED_GUILTY_COURT_HEARING_REQUESTED_MUTATOR)
                .put(PleadedNotGuilty.class, PLEADED_NOT_GUILTY_MUTATOR)
                .put(DecisionSaved.class, DECISION_MUTATOR)
                .put(PleasSet.class, PLEAS_SET_MUTATOR)
                .put(DatesToAvoidTimerExpired.class, DATES_TO_AVOID_TIMER_EXPIRED_MUTATOR)
                .put(DefendantResponseTimerExpired.class, DEFENDANT_RESPONSE_TIMER_EXPIRED)
                .put(DatesToAvoidRequired.class, DATES_TO_AVOID_REQUIRED)
                .build();
    }

    @Override
    public void apply(final Object event, final CaseAggregateState aggregateState) {
        Optional.ofNullable(eventToStateMutator.get(event.getClass()))
                .ifPresent(stateMutator -> stateMutator.apply(event, aggregateState));
    }

}
