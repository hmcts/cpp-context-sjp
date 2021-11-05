package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.DELEGATED_POWERS_DECISION;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_PLEA;
import static uk.gov.moj.cpp.sjp.domain.aggregate.domain.SessionRules.getPriority;
import static uk.gov.moj.cpp.sjp.domain.aggregate.domain.SessionRules.getSessionType;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_ASSIGNED_TO_OTHER_USER;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_COMPLETED;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_NOT_READY;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.SessionRules;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusUpdated;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyReopened;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;
import uk.gov.moj.cpp.sjp.event.CaseExpectedDateReadyChanged;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsV2;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseNotReopened;
import uk.gov.moj.cpp.sjp.event.CaseOffenceListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUndone;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUpdated;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidAdded;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidTimerExpired;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantResponseTimerExpired;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseAlreadyAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassignmentRejected;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseCoreHandler {

    public static final CaseCoreHandler INSTANCE = new CaseCoreHandler();

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseCoreHandler.class);

    private CaseCoreHandler() {
    }

    public Stream<Object> receiveCase(final Case aCase,
                                      final ZonedDateTime createdOn,
                                      final CaseAggregateState state) {

        final Object event;

        if (state.isCaseReceived()) {
            event = new CaseCreationFailedBecauseCaseAlreadyExisted(state.getCaseId(), state.getUrn());
        } else {
            final Defendant defendant = new Defendant.DefendantBuilder()
                    .withId(UUID.randomUUID())
                    .buildBasedFrom(aCase.getDefendant());

            final LocalDate expectedDateReady = aCase.getPostingDate().plusDays(NUMBER_DAYS_WAITING_FOR_PLEA);

            event = new CaseReceived(
                    aCase.getId(),
                    aCase.getUrn(),
                    aCase.getEnterpriseId(),
                    aCase.getProsecutingAuthority(),
                    aCase.getCosts(),
                    aCase.getPostingDate(),
                    defendant,
                    expectedDateReady,
                    createdOn);
        }
        return Stream.of(event);
    }

    public Stream<Object> updateCaseListedInCriminalCourts(final UUID caseId, final String hearingCourtName,
                                                           final ZonedDateTime hearingTime) {
        return Stream.of(new CaseListedInCriminalCourts(caseId, hearingCourtName, hearingTime));
    }

    public Stream<Object> updateCaseOffenceListedInCcForReferToCourt(final UUID caseId,
                                                                     final UUID defendantId,
                                                                     final List<UUID> defendantOffences,
                                                                     final UUID hearingId,
                                                                     final CourtCentre courtCentre,
                                                                     final List<HearingDay> hearingDays) {
        return Stream.of(new CaseOffenceListedInCriminalCourts(caseId, defendantId,defendantOffences, hearingId, courtCentre, hearingDays));
    }

    public Stream<Object> updateCaseListedInCcForReferToCourt(final List<CaseOffenceListedInCriminalCourts> offenceHearings,
                                                              final DecisionSaved decisionSaved,
                                                              final UUID caseId) {
        return Stream.of(new CaseListedInCriminalCourtsV2(offenceHearings, decisionSaved, caseId));
    }

    public Stream<Object> addDatesToAvoid(final String datesToAvoid,
                                          final CaseAggregateState state, final String userProsecutingAuthority) {

        return createRejectionEvents(
                null,
                "Add dates to avoid",
                null,
                state,
                userProsecutingAuthority
        ).orElse(Stream.of(state.getDatesToAvoid() == null ?
                new DatesToAvoidAdded(state.getCaseId(), datesToAvoid) :
                new DatesToAvoidUpdated(state.getCaseId(), datesToAvoid)));
    }

    public Stream<Object> expireDatesToAvoidTimer(final CaseAggregateState state) {
        return Stream.of(new DatesToAvoidTimerExpired(state.getCaseId()));
    }

    public Stream<Object> expireDefendantResponseTimer(final CaseAggregateState state) {
        return Stream.of(new DefendantResponseTimerExpired(state.getCaseId()));
    }

    public Stream<Object> markCaseReopened(final CaseReopenDetails caseReopenDetails,
                                           final CaseAggregateState state) {

        return Stream.of(
                createCaseNotFoundEventForWrongCaseId(
                        caseReopenDetails.getCaseId(),
                        "Mark case reopened",
                        state
                ).orElseGet(() -> markCaseReopenedIfNotAlreadyReopened(caseReopenDetails, state)));
    }

    private Object markCaseReopenedIfNotAlreadyReopened(final CaseReopenDetails caseReopenDetails, final CaseAggregateState state) {
        if (state.isCaseReopened()) {
            LOGGER.warn("Cannot reopen case. Case already reopened with ID {}", state.getCaseId());
            return new CaseAlreadyReopened(caseReopenDetails.getCaseId(), "Cannot mark case reopened");
        } else {
            return new CaseReopened(caseReopenDetails);
        }
    }

    public Stream<Object> updateCaseReopened(final CaseReopenDetails caseReopenDetails,
                                             final CaseAggregateState state) {

        return Stream.of(
                createCaseNotFoundEventForWrongCaseId(
                        caseReopenDetails.getCaseId(),
                        "Update case reopened",
                        state
                ).orElseGet(() -> createUpdatedEventIfCaseReopened(caseReopenDetails, state)));
    }

    public Stream<Object> updateCaseStatusOnCCApplicationResult(final CaseAggregateState state, final UUID caseId, final UUID applicationId, final ApplicationStatus applicationStatus) {
        if(state.isCaseCompleted()) {
            return Stream.of(new CCApplicationStatusUpdated(caseId, applicationId, applicationStatus));
        } else {
            return Stream.empty();
        }

    }

    private Object createUpdatedEventIfCaseReopened(final CaseReopenDetails caseReopenDetails,
                                                    final CaseAggregateState state) {

        if (state.isCaseReopened()) {
            return new CaseReopenedUpdated(caseReopenDetails);
        } else {
            LOGGER.warn("Cannot update reopened case. Case not yet reopened with ID {}", state.getCaseId());
            return new CaseNotReopened(caseReopenDetails.getCaseId(), "Cannot update case reopened");
        }
    }

    public Stream<Object> undoCaseReopened(final UUID caseId, final CaseAggregateState state) {
        return Stream.of(createCaseNotFoundEventForWrongCaseId(caseId, "Undo case reopened", state)
                .orElseGet(() -> createCaseReopenedUndoneEventIfCaseReopened(caseId, state)));
    }

    private Object createCaseReopenedUndoneEventIfCaseReopened(final UUID caseId,
                                                               final CaseAggregateState state) {

        if (!state.isCaseReopened() || state.getCaseReopenedDate() == null) {
            LOGGER.warn("Cannot undo reopened case. Case not yet reopened with ID: {}", caseId);
            return new CaseNotReopened(caseId, "Cannot undo case reopened");
        } else {
            return new CaseReopenedUndone(caseId, state.getCaseReopenedDate());
        }
    }

    public Stream<Object> assignCase(final UUID assigneeId,
                                     final ZonedDateTime assignedAt,
                                     final CaseAssignmentType assignmentType,
                                     final CaseAggregateState state) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

    if (state.isCaseCompleted() && !state.hasPendingApplication()) {
            streamBuilder.add(new CaseAssignmentRejected(CASE_COMPLETED));
        } else if (state.getAssigneeId() != null) {
            checkIfCaseAlreadyAssignedOrAssigneeIdInvalid(assigneeId, state, streamBuilder);
        } else {
            streamBuilder.add(new CaseAssigned(state.getCaseId(), assigneeId, assignedAt, assignmentType));
        }

        return streamBuilder.build();
    }

    public Stream<Object> assignCaseToUser(final UUID assigneeId,
                                           final ZonedDateTime assignedAt,
                                           final CaseAggregateState state) {
        if (!state.isCaseReadyForDecision()) {
            return Stream.of(new CaseAssignmentRejected(CASE_NOT_READY));
        }

        final CaseReadinessReason caseReadinessReason = state.getReadinessReason();
        final SessionType sessionType = SessionRules.getSessionType(caseReadinessReason, state.isPostConviction(), state.isSetAside(),state.hasPendingApplication());
        final CaseAssignmentType caseAssignmentType = sessionType == SessionType.MAGISTRATE ? MAGISTRATE_DECISION : DELEGATED_POWERS_DECISION;

        if (assigneeId.equals(state.getAssigneeId())) {
            return Stream.of(new CaseAlreadyAssigned(state.getCaseId(), assigneeId));
        }
        if (state.getAssigneeId() == null) {
            return Stream.of(new CaseAssigned(state.getCaseId(), assigneeId, assignedAt, caseAssignmentType));
        }

        return Stream.of(new CaseUnassigned(state.getCaseId()), new CaseAssigned(state.getCaseId(), assigneeId, assignedAt, caseAssignmentType));
    }

    private void checkIfCaseAlreadyAssignedOrAssigneeIdInvalid(final UUID assigneeId,
                                                               final CaseAggregateState state,
                                                               final Stream.Builder<Object> streamBuilder) {

        if (!state.getAssigneeId().equals(assigneeId)) {
            streamBuilder.add(new CaseAssignmentRejected(CASE_ASSIGNED_TO_OTHER_USER));
        } else {
            streamBuilder.add(new CaseAlreadyAssigned(state.getCaseId(), assigneeId));
        }
    }

    public Stream<Object> unassignCase(final CaseAggregateState state) {
        return state.getAssigneeId() != null
                ? Stream.of(new CaseUnassigned(state.getCaseId()))
                : Stream.of(new CaseUnassignmentRejected(CaseUnassignmentRejected.RejectReason.CASE_NOT_ASSIGNED));
    }

    public Stream<Object> markCaseReadyForDecision(final CaseReadinessReason readinessReason,
                                                   final ZonedDateTime markedAt,
                                                   final CaseAggregateState state) {

        return state.getReadinessReason() != readinessReason
                ? Stream.of(new CaseMarkedReadyForDecision(state.getCaseId(), readinessReason, markedAt, getSessionType(readinessReason, state.isPostConviction(), state.isSetAside(),state.hasPendingApplication()), getPriority(state)))
                : Stream.of();
    }

    public Stream<Object> unmarkCaseReadyForDecision(final LocalDate expectedDateReady, final CaseAggregateState state) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (state.isCaseReadyForDecision()) {
            streamBuilder.add(new CaseUnmarkedReadyForDecision(state.getCaseId(), expectedDateReady));
        } else if (!expectedDateReady.equals(state.getExpectedDateReady())) {
            streamBuilder.add(new CaseExpectedDateReadyChanged(state.getCaseId(), state.getExpectedDateReady(), expectedDateReady));
        }

        return streamBuilder.build();
    }

    private Optional<Object> createCaseNotFoundEventForWrongCaseId(final UUID caseId,
                                                                   final String action,
                                                                   final CaseAggregateState state) {
        if (!state.isCaseIdEqualTo(caseId)) {
            LOGGER.error("Mismatch of IDs in aggregate: {} != {}", state.getCaseId(), caseId);
            return Optional.of(new CaseNotFound(caseId, action));
        } else {
            return Optional.empty();
        }
    }
}
