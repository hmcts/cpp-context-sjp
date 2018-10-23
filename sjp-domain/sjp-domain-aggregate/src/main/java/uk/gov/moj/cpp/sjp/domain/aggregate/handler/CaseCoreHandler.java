package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_ASSIGNED_TO_OTHER_USER;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_COMPLETED;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyCompleted;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyReopened;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseNotReopened;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUndone;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUpdated;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidAdded;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidUpdated;
import uk.gov.moj.cpp.sjp.event.session.CaseAlreadyAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassignmentRejected;

import java.time.ZonedDateTime;
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

            event = new CaseReceived(
                    aCase.getId(),
                    aCase.getUrn(),
                    aCase.getEnterpriseId(),
                    aCase.getProsecutingAuthority(),
                    aCase.getCosts(),
                    aCase.getPostingDate(),
                    defendant,
                    createdOn);
        }

        return Stream.of(event);
    }

    public Stream<Object> completeCase(final CaseAggregateState state) {
        final UUID caseId = state.getCaseId();
        if (state.isCaseCompleted()) {
            LOGGER.warn("Case has already been completed {}", caseId);
            return Stream.of(new CaseAlreadyCompleted(caseId, "Complete Case"));
        }

        return Stream.of(new CaseUnassigned(caseId), new CaseCompleted(caseId));
    }

    public Stream<Object> addDatesToAvoid(final String datesToAvoid,
                                          final CaseAggregateState state) {

        return createRejectionEvents(
                null,
                "Add dates to avoid",
                null,
                state
        ).orElse(Stream.of(state.getDatesToAvoid() == null ?
                new DatesToAvoidAdded(state.getCaseId(), datesToAvoid) :
                new DatesToAvoidUpdated(state.getCaseId(), datesToAvoid)));
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

        if (state.isCaseCompleted()) {
            streamBuilder.add(new CaseAssignmentRejected(CASE_COMPLETED));
        } else if (state.getAssigneeId() != null) {
            checkIfCaseAlreadyAssignedOrAsigneeIdInvalid(assigneeId, state, streamBuilder);
        } else {
            streamBuilder.add(new CaseAssigned(state.getCaseId(), assigneeId, assignedAt, assignmentType));
        }

        return streamBuilder.build();
    }

    private void checkIfCaseAlreadyAssignedOrAsigneeIdInvalid(final UUID assigneeId,
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
                ? Stream.of(new CaseMarkedReadyForDecision(state.getCaseId(), readinessReason, markedAt))
                : Stream.of();
    }

    public Stream<Object> unmarkCaseReadyForDecision(CaseAggregateState state) {
        return state.getReadinessReason() != null
                ? Stream.of(new CaseUnmarkedReadyForDecision(state.getCaseId()))
                : Stream.of();
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
