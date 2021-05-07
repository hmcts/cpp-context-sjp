package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Stream.empty;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.json.schemas.domains.sjp.Note;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.aggregate.casestatus.CaseStatusResolver;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseAdjournmentHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseCoreHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseDecisionHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseDefendantHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseDocumentHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseEmployerHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseFinancialMeansHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseLanguageHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseNoteHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseReadinessHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.ChangeCaseManagementStatusHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CourtReferralHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.DeleteDocsHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.OffenceWithdrawalHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.ResolveCaseStatusHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.SetDatesToAvoidRequiredAggregateHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.UpdateAllFinancialMeansAggregateHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.OnlinePleaHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.SetPleasHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.mutator.AggregateStateMutator;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;
import uk.gov.moj.cpp.sjp.domain.common.CaseState;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.SetPleas;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public class CaseAggregate implements Aggregate {

    private static final long serialVersionUID = 13L;
    private static final AggregateStateMutator<Object, CaseAggregateState> AGGREGATE_STATE_MUTATOR = AggregateStateMutator.compositeCaseAggregateStateMutator();
    private static final CaseReadinessHandler caseReadinessHandler = CaseReadinessHandler.INSTANCE;

    @SuppressWarnings("squid:S1948")
    private final CaseAggregateState state = new CaseAggregateState();

    public Stream<Object> receiveCase(final Case aCase, final ZonedDateTime createdOn) {
        return apply(CaseCoreHandler.INSTANCE.receiveCase(aCase, createdOn, state));
    }

    public Stream<Object> updateCaseListedInCriminalCourts(final UUID caseId,
                                                           final String hearingCourtName,
                                                           final ZonedDateTime hearingTime) {
        if (state.isCaseReceived()) {
            return apply(CaseCoreHandler.INSTANCE.updateCaseListedInCriminalCourts(caseId, hearingCourtName, hearingTime));
        }
        return empty();
    }

    public Stream<Object> markCaseReopened(final CaseReopenDetails caseReopenDetails) {
        return applyAndResolveCaseReadiness(() -> CaseCoreHandler.INSTANCE.markCaseReopened(caseReopenDetails, state));
    }

    public Stream<Object> updateCaseReopened(final CaseReopenDetails caseReopenDetails) {
        return applyAndResolveCaseReadiness(() -> CaseCoreHandler.INSTANCE.updateCaseReopened(caseReopenDetails, state));
    }

    /**
     * @deprecated The CaseMarkedReadyForDecision is raised from {@link CaseReadinessHandler#resolveCaseReadiness}
     */
    @Deprecated
    public Stream<Object> markCaseReadyForDecision(final CaseReadinessReason readinessReason, final ZonedDateTime markedAt) {
        return apply(CaseCoreHandler.INSTANCE.markCaseReadyForDecision(readinessReason, markedAt, state));
    }

    /**
     * @deprecated The CaseUnmarkedReadyForDecision and CaseExpectedDateReadyChanged events are
     * raised from {@link CaseReadinessHandler#resolveCaseReadiness}
     */
    @Deprecated
    public Stream<Object> unmarkCaseReadyForDecision(final LocalDate expectedDateReady) {
        return apply(CaseCoreHandler.INSTANCE.unmarkCaseReadyForDecision(expectedDateReady, state));
    }

    public Stream<Object> undoCaseReopened(final UUID caseId) {
        return applyAndResolveCaseReadiness(() -> CaseCoreHandler.INSTANCE.undoCaseReopened(caseId, state));
    }

    public Stream<Object> assignCase(final UUID assigneeId, final ZonedDateTime assignedAt, final CaseAssignmentType assignmentType) {
        return apply(CaseCoreHandler.INSTANCE.assignCase(assigneeId, assignedAt, assignmentType, state));
    }

    public Stream<Object> assignCaseToUser(final UUID assigneeId, final ZonedDateTime assignedAt) {
        return apply(CaseCoreHandler.INSTANCE.assignCaseToUser(assigneeId, assignedAt, state));
    }

    public Stream<Object> unassignCase() {
        return apply(CaseCoreHandler.INSTANCE.unassignCase(state));
    }

    public Stream<Object> updateFinancialMeans(final UUID userId, final FinancialMeans financialMeans) {
        return apply(CaseFinancialMeansHandler.INSTANCE.updateFinancialMeans(userId, financialMeans, state));
    }

    public Stream<Object> deleteFinancialMeans(final UUID defendantId) {
        return apply(CaseFinancialMeansHandler.INSTANCE.deleteFinancialMeans(defendantId, state));
    }

    public Stream<Object> updateEmployer(final UUID userId, final Employer employer) {
        return apply(CaseEmployerHandler.INSTANCE.updateEmployer(userId, employer, state));
    }

    public Stream<Object> deleteEmployer(final UUID userId, final UUID defendantId) {
        return apply(CaseEmployerHandler.INSTANCE.deleteEmployer(userId, defendantId, state));
    }

    public Stream<Object> addCaseDocument(final UUID caseId, final CaseDocument caseDocument) {
        return apply(CaseDocumentHandler.INSTANCE.addCaseDocument(caseId, caseDocument, state));
    }

    public Stream<Object> uploadCaseDocument(final UUID caseId, final UUID documentReference, final String documentType) {
        return apply(CaseDocumentHandler.INSTANCE.uploadCaseDocument(caseId, documentReference, documentType, state));
    }

    public Stream<Object> updateHearingRequirements(final UUID userId,
                                                    final UUID defendantId,
                                                    final String interpreterLanguage,
                                                    final Boolean speakWelsh) {

        return apply(CaseLanguageHandler.INSTANCE.updateHearingRequirements(userId, defendantId, interpreterLanguage, speakWelsh, state, PleaMethod.POSTAL, null));
    }

    public Stream<Object> updateHearingRequirements(final UUID userId,
                                                    final UUID defendantId,
                                                    final String interpreterLanguage,
                                                    final Boolean speakWelsh,
                                                    final PleaMethod pleaMethod,
                                                    final ZonedDateTime createdOn) {

        return apply(CaseLanguageHandler.INSTANCE.updateHearingRequirements(userId, defendantId, interpreterLanguage, speakWelsh, state, pleaMethod, createdOn));
    }

    public Stream<Object> addDatesToAvoid(final String datesToAvoid, final String userProsecutingAuthority) {
            return applyAndResolveCaseReadiness(() -> CaseCoreHandler.INSTANCE.addDatesToAvoid(datesToAvoid, state, userProsecutingAuthority));
    }

    public Stream<Object> expireDefendantResponseTimer() {
        return applyAndResolveCaseReadiness(() -> CaseCoreHandler.INSTANCE.expireDefendantResponseTimer(state));
    }

    public Stream<Object> expireDatesToAvoidTimer() {
        return applyAndResolveCaseReadiness(() -> CaseCoreHandler.INSTANCE.expireDatesToAvoidTimer(state));
    }

    public Stream<Object> updateDefendantNationalInsuranceNumber(final UUID userId, final UUID defendantId, final String newNationalInsuranceNumber) {
        return apply(CaseDefendantHandler.INSTANCE.updateDefendantNationalInsuranceNumber(userId, defendantId, newNationalInsuranceNumber, state));
    }

    public Stream<Object> pleadOnline(final UUID caseId, final PleadOnline pleadOnline, final ZonedDateTime createdOn, final UUID userId) {
        return applyAndResolveCaseReadiness(() -> OnlinePleaHandler.INSTANCE.pleadOnline(caseId, pleadOnline, createdOn, state, userId));
    }

    public Stream<Object> acknowledgeDefendantDetailsUpdates(
            final UUID defendantId,
            final ZonedDateTime acknowledgedAt,
            final String userProsecutingAuthority) {

        return apply(CaseDefendantHandler.INSTANCE.acknowledgeDefendantDetailsUpdates(defendantId, acknowledgedAt, state, userProsecutingAuthority));
    }

    public Stream<Object> updateDefendantDetails(final UUID userId,
                                                 final UUID caseId,
                                                 final UUID defendantId,
                                                 final Person person,
                                                 final ZonedDateTime updatedDate) {

        return apply(CaseDefendantHandler.INSTANCE.updateDefendantDetails(userId, caseId, defendantId, person, updatedDate, state));
    }

    public Stream<Object> recordCaseReferralForCourtHearingRejection(final UUID caseId,
                                                                     final String rejectionReason,
                                                                     final ZonedDateTime rejectedAt) {

        return apply(CourtReferralHandler.INSTANCE.recordCaseReferralForCourtHearingRejection(
                caseId,
                rejectionReason,
                rejectedAt,
                state));
    }

    public Stream<Object> recordCaseAdjournedToLaterSjpHearing(final UUID caseId,
                                                               final UUID sessionId,
                                                               final LocalDate adjournedTo) {

        return applyAndResolveCaseReadiness(() -> CaseAdjournmentHandler.INSTANCE.recordCaseAdjournedToLaterSjpHearing(
                caseId,
                sessionId,
                adjournedTo,
                state));
    }

    public Stream<Object> recordCaseAdjournmentToLaterSjpHearingElapsed(final UUID caseId, final ZonedDateTime elapsedAt) {

        return applyAndResolveCaseReadiness(() -> CaseAdjournmentHandler.INSTANCE.recordCaseAdjournmentToLaterSjpHearingElapsed(
                caseId,
                elapsedAt,
                state));
    }

    public Stream<Object> addCaseNote(final UUID caseId, final Note note, final User author, final UUID decisionId) {
        return apply(CaseNoteHandler.INSTANCE.addCaseNote(
                caseId,
                note,
                author,
                decisionId,
                state));
    }

    public Stream<Object> requestForOffenceWithdrawal(final UUID caseId, final UUID setBy, final ZonedDateTime setAt, final List<WithdrawalRequestsStatus> withdrawalRequestsStatus, final String prosecutionAuthority) {
        return applyAndResolveCaseReadiness(() -> OffenceWithdrawalHandler.INSTANCE.requestOffenceWithdrawal(caseId, setBy, setAt, withdrawalRequestsStatus, state, prosecutionAuthority));
    }

    public Stream<Object> setPleas(final UUID caseId, final SetPleas pleas, final UUID userId, final ZonedDateTime pleadAt) {
        return applyAndResolveCaseReadiness(() -> SetPleasHandler.INSTANCE.setPleas(caseId, pleas, state, userId, pleadAt));
    }

    public Stream<Object> updateAllFinancialMeans(final UUID userId,
                                                  final UUID defendantId,
                                                  final Income income,
                                                  final Benefits benefits,
                                                  final Employer employer,
                                                  final String employmentStatus) {
        return apply(UpdateAllFinancialMeansAggregateHandler.INSTANCE.updateAllFinancialMeans(
                userId, defendantId, income, benefits, employer, employmentStatus, state));
    }

    public Stream<Object> saveDecision(final Decision decision, final Session session) {
        return applyAndResolveCaseReadiness(() -> CaseDecisionHandler.INSTANCE.saveDecision(decision, state, session));
    }

    public Stream<Object> setDatesToAvoidRequired() {
        return applyAndResolveCaseReadiness(() -> SetDatesToAvoidRequiredAggregateHandler.INSTANCE.handleSetDatesToAvoidRequired(state));
    }

    public Stream<Object> resolveCaseStatus() {
        return apply(ResolveCaseStatusHandler.INSTANCE.resolveCaseStatus(state, CaseStatusResolver.resolve(state)));
    }

    public Stream<Object> changeCaseManagementStatus(final CaseManagementStatus caseManagementStatus) {
        return apply(ChangeCaseManagementStatusHandler.INSTANCE.changeCaseManagementStatus(state, caseManagementStatus));
    }

    public Stream<Object> deleteDocs() {
        return apply(DeleteDocsHandler.INSTANCE.deleteDocs(state));
    }

    private Stream<Object> applyAndResolveCaseReadiness(final Supplier<Stream<Object>> streamSupplier) {
        final CaseState previousCaseState = CaseStatusResolver.resolve(state);
        final Stream<Object> stream = apply(streamSupplier.get());
        final CaseState currentCaseState = CaseStatusResolver.resolve(state);
        final Stream<Object> caseReadinessStream = caseReadinessHandler.resolveCaseReadiness(state, previousCaseState, currentCaseState);
        return Stream.concat(stream, caseReadinessStream);
    }

    public CaseAggregateState getState() {
        return state;
    }

    @Override
    public Object apply(final Object event) {
        AGGREGATE_STATE_MUTATOR.apply(event, state);
        return event;
    }


}
