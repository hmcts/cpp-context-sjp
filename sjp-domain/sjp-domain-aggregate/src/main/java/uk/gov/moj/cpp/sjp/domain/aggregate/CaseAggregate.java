package uk.gov.moj.cpp.sjp.domain.aggregate;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseCoreHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseDefendantHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseDocumentHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseEmployerHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseFinancialMeansHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseLanguageHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseWithdrawalHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CourtReferralHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.CancelPleaHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.OnlinePleaHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.UpdatePleaHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.mutator.AggregateStateMutator;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public class CaseAggregate implements Aggregate {

    private static final long serialVersionUID = 7L;

    @SuppressWarnings("squid:S1948")
    private final CaseAggregateState state = new CaseAggregateState();
    private static final AggregateStateMutator<Object, CaseAggregateState> AGGREGATE_STATE_MUTATOR = AggregateStateMutator.compositeCaseAggregateStateMutator();

    public Stream<Object> receiveCase(final Case aCase, final ZonedDateTime createdOn) {
        return apply(CaseCoreHandler.INSTANCE.receiveCase(aCase, createdOn, state));
    }

    public Stream<Object> completeCase() {
        return apply(CaseCoreHandler.INSTANCE.completeCase(state));
    }

    public Stream<Object> markCaseReopened(final CaseReopenDetails caseReopenDetails) {
        return apply(CaseCoreHandler.INSTANCE.markCaseReopened(caseReopenDetails, state));
    }

    public Stream<Object> updateCaseReopened(final CaseReopenDetails caseReopenDetails) {
        return apply(CaseCoreHandler.INSTANCE.updateCaseReopened(caseReopenDetails, state));
    }

    public Stream<Object> markCaseReadyForDecision(final CaseReadinessReason readinessReason, final ZonedDateTime markedAt) {
        return apply(CaseCoreHandler.INSTANCE.markCaseReadyForDecision(readinessReason, markedAt, state));
    }

    public Stream<Object> unmarkCaseReadyForDecision() {
        return apply(CaseCoreHandler.INSTANCE.unmarkCaseReadyForDecision(state));
    }

    public Stream<Object> undoCaseReopened(final UUID caseId) {
        return apply(CaseCoreHandler.INSTANCE.undoCaseReopened(caseId, state));
    }

    public Stream<Object> assignCase(final UUID assigneeId, final ZonedDateTime assignedAt, final CaseAssignmentType assignmentType) {
        return apply(CaseCoreHandler.INSTANCE.assignCase(assigneeId, assignedAt, assignmentType, state));
    }

    public Stream<Object> unassignCase() {
        return apply(CaseCoreHandler.INSTANCE.unassignCase(state));
    }

    public Stream<Object> updateFinancialMeans(final UUID userId, final FinancialMeans financialMeans) {
        return apply(CaseFinancialMeansHandler.INSTANCE.updateFinancialMeans(userId, financialMeans, state));
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

        return apply(CaseLanguageHandler.INSTANCE.updateHearingRequirements(userId, defendantId, interpreterLanguage, speakWelsh, state));
    }

    public Stream<Object> addDatesToAvoid(final String datesToAvoid) {
        return apply(CaseCoreHandler.INSTANCE.addDatesToAvoid(datesToAvoid, state));
    }

    public Stream<Object> updateDefendantNationalInsuranceNumber(final UUID userId, final UUID defendantId, final String newNationalInsuranceNumber) {
        return apply(CaseDefendantHandler.INSTANCE.updateDefendantNationalInsuranceNumber(userId, defendantId, newNationalInsuranceNumber, state));
    }

    public Stream<Object> updatePlea(final UUID userId, final UpdatePlea updatePleaCommand, final ZonedDateTime updatedOn) {
        return apply(UpdatePleaHandler.INSTANCE.updatePlea(userId, updatePleaCommand, updatedOn, state));
    }

    public Stream<Object> cancelPlea(final UUID userId, final CancelPlea cancelPleaCommand) {
        return apply(CancelPleaHandler.INSTANCE.cancelPlea(userId, cancelPleaCommand, state));
    }

    public Stream<Object> pleadOnline(final UUID caseId, final PleadOnline pleadOnline, final ZonedDateTime createdOn) {
        return apply(OnlinePleaHandler.INSTANCE.pleadOnline(caseId, pleadOnline, createdOn, state));
    }

    public Stream<Object> requestWithdrawalAllOffences() {
        return apply(CaseWithdrawalHandler.INSTANCE.requestWithdrawalAllOffences(state));
    }

    public Stream<Object> cancelRequestWithdrawalAllOffences() {
        return apply(CaseWithdrawalHandler.INSTANCE.cancelRequestWithdrawalAllOffences(state));
    }

    public Stream<Object> acknowledgeDefendantDetailsUpdates(
            final UUID defendantId,
            final ZonedDateTime acknowledgedAt) {

        return apply(CaseDefendantHandler.INSTANCE.acknowledgeDefendantDetailsUpdates(defendantId, acknowledgedAt, state));
    }

    public Stream<Object> updateDefendantDetails(final UUID caseId,
                                                 final UUID defendantId,
                                                 final Person person,
                                                 final ZonedDateTime updatedDate) {

        return apply(CaseDefendantHandler.INSTANCE.updateDefendantDetails(caseId, defendantId, person, updatedDate, state));
    }

    public Stream<Object> referCaseForCourtHearing(final UUID caseId,
                                                   final UUID sessionId,
                                                   final UUID referralReasonId,
                                                   final UUID hearingTypeId,
                                                   final Integer estimatedHearingDuration,
                                                   final String listingNotes,
                                                   final ZonedDateTime requestedAt
    ) {
        return apply(CourtReferralHandler.INSTANCE.referCaseForCourtHearing(caseId,
                sessionId,
                referralReasonId,
                hearingTypeId,
                listingNotes,
                estimatedHearingDuration,
                requestedAt,
                state));
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

    @Override
    public Object apply(Object event) {
        AGGREGATE_STATE_MUTATOR.apply(event, state);
        return event;
    }
}
