package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.REOPENING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;

import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.FinancialImpositionExportDetails;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.ApplicationResultsRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2;
import uk.gov.moj.cpp.sjp.event.CaseReserved;
import uk.gov.moj.cpp.sjp.event.CaseUnReserved;
import uk.gov.moj.cpp.sjp.event.DefendantAocpResponseTimerExpired;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusUpdated;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseAdjournmentToLaterSjpHearingElapsed;
import uk.gov.moj.cpp.sjp.event.CaseApplicationForReopeningRecorded;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseEligibleForAOCP;
import uk.gov.moj.cpp.sjp.event.CaseExpectedDateReadyChanged;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsV2;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseOffenceListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUndone;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUpdated;
import uk.gov.moj.cpp.sjp.event.CaseStarted;
import uk.gov.moj.cpp.sjp.event.CaseStatDecRecorded;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidAdded;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidTimerExpired;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantAcceptedAocp;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantResponseTimerExpired;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAdded;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAddedBdf;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionCorrelationIdAdded;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsStarted;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleted;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestReasonChanged;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.PaymentTermsChanged;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.TrialRequested;
import uk.gov.moj.cpp.sjp.event.VerdictCancelled;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.decision.ConvictionCourtResolved;
import uk.gov.moj.cpp.sjp.event.decision.DecisionResubmitted;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSetAside;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSetAsideReset;
import uk.gov.moj.cpp.sjp.event.decommissioned.CaseAssignmentDeleted;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;

/**
 * Defines the composite {@link AggregateStateMutator} which delegates to the appropriate mutator
 * based on the event handled.
 */
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
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
                state.removePlea(event.getDefendantId(), event.getOffenceId());
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
            (event, state) -> {
                state.markCaseReferredForCourtHearing();
                final DisabilityNeeds disabilityNeeds = ofNullable(event.getDefendantCourtOptions())
                        .map(DefendantCourtOptions::getDisabilityNeeds)
                        .orElse(NO_DISABILITY_NEEDS);
                state.updateDefendantDisabilityNeeds(state.getDefendantId(), disabilityNeeds);
                state.setManagedByAtcm(false);
            };
    private static final AggregateStateMutator<CaseReferredForCourtHearingV2, CaseAggregateState> CASE_REFERRED_FOR_COURT_HEARING_MUTATOR_V2 =
            (event, state) -> {
                state.markCaseReferredForCourtHearing();
                final DisabilityNeeds disabilityNeeds = ofNullable(event.getDefendantCourtOptions())
                        .map(DefendantCourtOptions::getDisabilityNeeds)
                        .orElse(NO_DISABILITY_NEEDS);
                state.updateDefendantDisabilityNeeds(state.getDefendantId(), disabilityNeeds);
                state.setManagedByAtcm(false);
            };
    private static final AggregateStateMutator<CaseReferralForCourtHearingRejectionRecorded, CaseAggregateState> CASE_REFERRED_FOR_COURT_HEARING_REJECTION_MUTATOR =
            (event, state) -> state.setManagedByAtcm(true);
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
            ((event, state) -> {
                state.updateOffenceDecisions(event.getOffenceDecisions(), event.getSessionId());
                state.updateOffenceConvictionDetails(event.getSavedAt(), event.getOffenceDecisions(), event.getSessionId());

                // DD-16405
                if (event.getFinancialImposition() != null) {
                    state.setDecisionSavedWithFinancialImposition(event);
                }
                if (event
                        .getOffenceDecisions()
                        .stream()
                        .filter(e -> e instanceof ReferForCourtHearing)
                        .count() > 0) {
                    state.setLatestReferToCourtDecision(event);
                }
                final ZonedDateTime savedAtDate = event.getSavedAt();
                if(nonNull(savedAtDate)) {
                    state.setSavedAt(event.getSavedAt().toLocalDate());
                }
            });

    private static final AggregateStateMutator<ApplicationResultsRecorded, CaseAggregateState> APPLICATION_RESULTS_RECORDED =
            ((event, state) -> {
                final ApplicationResultsRecorded applicationResultsRecorded = ApplicationResultsRecorded.applicationResultsRecorded()
                        .withHearing(event.getHearing())
                        .withIsReshare(event.getIsReshare())
                        .withSharedTime(event.getSharedTime())
                        .withShadowListedOffences(event.getShadowListedOffences())
                        .withHearingDay(event.getHearingDay()).build();
                state.setApplicationResults(applicationResultsRecorded);
            });

    private static final AggregateStateMutator<ConvictionCourtResolved, CaseAggregateState> RESOLVE_CONVICTION_COURT_DETAILS_MUTATOR =
            ((event, state) -> state.resolveConvictionCourtDetails(event.getConvictingInformations()));


    private static final AggregateStateMutator<PleasSet, CaseAggregateState> PLEAS_SET_MUTATOR =
            ((event, state) -> {
                state.setPleas(new ArrayList<>(event.getPleas()));
                final DisabilityNeeds disabilityNeeds = ofNullable(event.getDefendantCourtOptions())
                        .map(DefendantCourtOptions::getDisabilityNeeds)
                        .orElse(NO_DISABILITY_NEEDS);
                state.updateDefendantDisabilityNeeds(state.getDefendantId(), disabilityNeeds);
                state.setDefendantAcceptedAocp(false);
            });

    private static final AggregateStateMutator<CaseAdjournedToLaterSjpHearingRecorded, CaseAggregateState> CASE_ADJOURNED_TO_LATER_HEARING_RECORDED_MUTATOR =
            ((event, state) -> state.setAdjournedTo(event.getAdjournedTo()));

    private static final AggregateStateMutator<CaseAdjournmentToLaterSjpHearingElapsed, CaseAggregateState> CASE_ADJOURNED_TO_LATER_HEARING_ELAPSED_MUTATOR =
            ((event, state) -> state.makeNonAdjourned());

    private static final AggregateStateMutator<DatesToAvoidTimerExpired, CaseAggregateState> DATES_TO_AVOID_TIMER_EXPIRED_MUTATOR =
            ((event, state) -> state.datesToAvoidTimerExpired());

    private static final AggregateStateMutator<DefendantResponseTimerExpired, CaseAggregateState> DEFENDANT_RESPONSE_TIMER_EXPIRED =
            ((event, state) -> state.setDefendantsResponseTimerExpired());

    private static final AggregateStateMutator<DefendantAocpResponseTimerExpired, CaseAggregateState> AOCP_ACCEPTANCE_RESPONSE_TIMER_EXPIRED =
            ((event, state) -> state.setAocpAcceptanceResponseTimerExpired());

    private static final AggregateStateMutator<CaseEligibleForAOCP, CaseAggregateState> CASE_ELIGIBLE_FOR_AOCP =
            ((event, state) -> {
                state.setAocpEligible();
                state.setAocpVictimSurcharge(event.getVictimSurcharge());
                state.setAocpTotalCost(event.getAocpTotalCost());
            });

    private static final AggregateStateMutator<DefendantAcceptedAocp, CaseAggregateState> DEFENDANT_ACCEPTED_AOCP =
            ((event, state) -> {
                state.setDefendantAcceptedAocp(true);
                state.setAocpAcceptedPleaDate(event.getPleadDate());
            });

    private static final AggregateStateMutator<DatesToAvoidRequired, CaseAggregateState> DATES_TO_AVOID_REQUIRED =
            ((event, state) -> {
                state.setDatesToAvoidExpirationDate(event.getDatesToAvoidExpirationDate());
                state.setDatesToAvoidPreviouslyRequested();
            });

    private static final AggregateStateMutator<DecisionSetAside, CaseAggregateState> DECISION_SET_ASIDE =
            ((event, state) -> state.setSetAside(true));

    private static final AggregateStateMutator<DecisionSetAsideReset, CaseAggregateState> DECISION_SET_ASIDE_RESET =
            ((event, state) -> state.setSetAside(false));

    private static final AggregateStateMutator<FinancialMeansDeleteDocsStarted, CaseAggregateState> DELETE_DOCS_STARTED =
            ((event, state) -> state.setDeleteDocsStarted(true));

    private static final AggregateStateMutator<CaseApplicationRecorded, CaseAggregateState> CASE_APPLICATION_RECORDED =
            ((event, state) -> {
                final Application application = new Application(event.getCourtApplication());
                state.setCurrentApplication(application);
            });

    private static final AggregateStateMutator<CaseStatDecRecorded, CaseAggregateState> CASE_STATDEC_RECORDED =
            ((event, state) -> state.getCurrentApplication().setType(STAT_DEC));

    private static final AggregateStateMutator<CaseApplicationForReopeningRecorded, CaseAggregateState> CASE_APPLICATION_FOR_REOPENING_RECORDED =
            ((event, state) -> state.getCurrentApplication().setType(REOPENING));

    private static final AggregateStateMutator<ApplicationStatusChanged, CaseAggregateState> APPLICATION_STATUS_CHANGED_MUTATOR =
            ((event, state) -> state.getCurrentApplication().setStatus(event.getStatus()));

    private static final AggregateStateMutator<VerdictCancelled, CaseAggregateState> VERDICT_CANCELLED_MUTATOR =
            ((event, state) -> state.removeOffenceConvictionDate(event.getOffenceId()));

    private static final AggregateStateMutator<ApplicationDecisionSetAside, CaseAggregateState> APPLICATION_DECISION_SET_ASIDE_MUTATOR =
            ((event, state) -> {
                state.setSetAside(true);
                state.unMarkCaseCompleted();
                state.clearOffenceDecisions();
                state.clearOffenceConvictionDates();
                state.setFinancialImpositionCorelationId(false);
            });

    private static final AggregateStateMutator<FinancialImpositionCorrelationIdAdded, CaseAggregateState> FINANCIAL_IMPOSITION_CORRELATION_ID_ADDED_MUTATOR =
            ((event, state) -> {
                final UUID defendantId = event.getDefendantId();
                final FinancialImpositionExportDetails fiExportDetails = ofNullable(state.getDefendantFinancialImpositionExportDetails(defendantId))
                        .orElse(new FinancialImpositionExportDetails());
                fiExportDetails.setCorrelationId(event.getCorrelationId());
                state.addFinancialImpositionExportDetails(defendantId, fiExportDetails);
                state.setFinancialImpositionCorelationId(true);
            });

    private static final AggregateStateMutator<FinancialImpositionAccountNumberAdded, CaseAggregateState> FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED_MUTATOR =
            ((event, state) -> {
                final UUID defendantId = event.getDefendantId();
                final FinancialImpositionExportDetails fiExportDetails = ofNullable(state.getDefendantFinancialImpositionExportDetails(defendantId))
                        .orElse(new FinancialImpositionExportDetails());
                fiExportDetails.setAccountNumber(event.getAccountNumber());
                state.addFinancialImpositionExportDetails(defendantId, fiExportDetails);
            });

    private static final AggregateStateMutator<FinancialImpositionAccountNumberAddedBdf, CaseAggregateState> FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED_MUTATOR_BDF =
            ((event, state) -> {
                final UUID defendantId = event.getDefendantId();
                final FinancialImpositionExportDetails fiExportDetails = ofNullable(state.getDefendantFinancialImpositionExportDetails(defendantId))
                        .orElse(new FinancialImpositionExportDetails());
                fiExportDetails.setAccountNumber(event.getAccountNumber());
                fiExportDetails.setCorrelationId(event.getCorrelationId());
                state.addFinancialImpositionExportDetails(defendantId, fiExportDetails);
            });

    private static final AggregateStateMutator<PaymentTermsChanged, CaseAggregateState> PAYMENT_TERMS_CHANGED_CASE_AGGREGATE_STATE_AGGREGATE_STATE_MUTATOR =
            ((event, state) -> state.setPaymentTermsUpdated(true));

    private static final AggregateStateMutator<DecisionResubmitted, CaseAggregateState> DECISION_RESUBMITTED_CASE_AGGREGATE_STATE_AGGREGATE_STATE_MUTATOR =
            ((event, state) -> state.setDecisionResubmitted(true));


    private static final AggregateStateMutator<CaseOffenceListedInCriminalCourts, CaseAggregateState> CASE_OFFENCE_LISTED_IN_CRIMINAL_COURTS_MUTATOR =
            ((event, state) -> state.updateOffenceHearings(event));

    private static final AggregateStateMutator<CaseListedInCriminalCourtsV2, CaseAggregateState> CASE_LISTED_IN_CRIMINAL_COURTS_V_2 =
            ((event, state) -> state.markCaseListed());

    private static final AggregateStateMutator<CaseReserved, CaseAggregateState> CASE_RESERVED =
            ((event, state) -> state.markCaseReserved());

    private static final AggregateStateMutator<CaseUnReserved, CaseAggregateState> CASE_UNRESERVED =
            ((event, state) -> state.markCaseUnReserved());

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
                .put(CaseReferredForCourtHearingV2.class, CASE_REFERRED_FOR_COURT_HEARING_MUTATOR_V2)
                .put(CaseReferralForCourtHearingRejectionRecorded.class, CASE_REFERRED_FOR_COURT_HEARING_REJECTION_MUTATOR)
                .put(CaseReopenedUndone.class, CaseReopenedUndoneMutator.INSTANCE)
                .put(CaseReopened.class, CaseReopenedMutator.INSTANCE)
                .put(CCApplicationStatusUpdated.class, UpdateCaseStatusOnCCApplicationResultMutator.INSTANCE)
                .put(CaseStarted.class, CASE_STARTED_MUTATOR)
                .put(CaseCompleted.class, CASE_COMPLETED_MUTATOR)
                .put(DatesToAvoidAdded.class, DATES_TO_AVOID_MUTATOR)
                .put(DatesToAvoidUpdated.class, DATES_TO_AVOID_UPDATED_MUTATOR)
                .put(DefendantDetailsUpdated.class, DefendantDetailsUpdatedMutator.INSTANCE)
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
                .put(ApplicationResultsRecorded.class,APPLICATION_RESULTS_RECORDED)
                .put(PleasSet.class, PLEAS_SET_MUTATOR)
                .put(DatesToAvoidTimerExpired.class, DATES_TO_AVOID_TIMER_EXPIRED_MUTATOR)
                .put(DefendantResponseTimerExpired.class, DEFENDANT_RESPONSE_TIMER_EXPIRED)
                .put(CaseEligibleForAOCP.class, CASE_ELIGIBLE_FOR_AOCP)
                .put(DefendantAcceptedAocp.class, DEFENDANT_ACCEPTED_AOCP)
                .put(DefendantAocpResponseTimerExpired.class, AOCP_ACCEPTANCE_RESPONSE_TIMER_EXPIRED)
                .put(DatesToAvoidRequired.class, DATES_TO_AVOID_REQUIRED)
                .put(DecisionSetAside.class, DECISION_SET_ASIDE)
                .put(DecisionSetAsideReset.class, DECISION_SET_ASIDE_RESET)
                .put(FinancialMeansDeleteDocsStarted.class, DELETE_DOCS_STARTED)
                .put(CaseApplicationRecorded.class, CASE_APPLICATION_RECORDED)
                .put(CaseStatDecRecorded.class, CASE_STATDEC_RECORDED)
                .put(CaseApplicationForReopeningRecorded.class, CASE_APPLICATION_FOR_REOPENING_RECORDED)
                .put(ApplicationStatusChanged.class, APPLICATION_STATUS_CHANGED_MUTATOR)
                .put(VerdictCancelled.class, VERDICT_CANCELLED_MUTATOR)
                .put(ApplicationDecisionSetAside.class, APPLICATION_DECISION_SET_ASIDE_MUTATOR)
                .put(FinancialImpositionCorrelationIdAdded.class, FINANCIAL_IMPOSITION_CORRELATION_ID_ADDED_MUTATOR)
                .put(FinancialImpositionAccountNumberAdded.class, FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED_MUTATOR)
                .put(FinancialImpositionAccountNumberAddedBdf.class, FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_ADDED_MUTATOR_BDF)
                .put(CaseOffenceListedInCriminalCourts.class, CASE_OFFENCE_LISTED_IN_CRIMINAL_COURTS_MUTATOR)
                .put(CaseListedInCriminalCourtsV2.class, CASE_LISTED_IN_CRIMINAL_COURTS_V_2)
                .put(PaymentTermsChanged.class, PAYMENT_TERMS_CHANGED_CASE_AGGREGATE_STATE_AGGREGATE_STATE_MUTATOR)
                .put(ConvictionCourtResolved.class, RESOLVE_CONVICTION_COURT_DETAILS_MUTATOR)
                .put(DecisionResubmitted.class, DECISION_RESUBMITTED_CASE_AGGREGATE_STATE_AGGREGATE_STATE_MUTATOR)
                .put(CaseReserved.class, CASE_RESERVED)
                .put(CaseUnReserved.class, CASE_UNRESERVED)
                .build();
    }

    @Override
    public void apply(final Object event, final CaseAggregateState aggregateState) {
        ofNullable(eventToStateMutator.get(event.getClass()))
                .ifPresent(stateMutator -> stateMutator.apply(event, aggregateState));
    }

}
