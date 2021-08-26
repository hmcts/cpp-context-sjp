package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.aggregate.casestatus.ExpectedDateReadyCalculator;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.common.CaseState;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.CaseExpectedDateReadyChanged;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static uk.gov.moj.cpp.sjp.domain.aggregate.domain.SessionRules.getPriority;
import static uk.gov.moj.cpp.sjp.domain.aggregate.domain.SessionRules.getSessionType;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED_APPLICATION_PENDING;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REOPENED_IN_LIBRA;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.isAReadyStatus;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.isAllowedStatusFromComplete;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.isAllowedStatusFromReopenedToLibra;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.isNotAllowedFromNonReady;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.isTerminalStatus;

public class CaseReadinessHandler {

    public static final CaseReadinessHandler INSTANCE = new CaseReadinessHandler();
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseReadinessHandler.class);
    private ExpectedDateReadyCalculator expectedDateReadyCalculator = new ExpectedDateReadyCalculator();

    private CaseReadinessHandler() {
        // singleton object
    }

    public Stream<Object> resolveCaseReadiness(final CaseAggregateState aggregateState,
                                               final CaseState previousCaseState,
                                               final CaseState currentCaseState) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final CaseStatus previousCaseStatus = previousCaseState.getCaseStatus();
        final CaseStatus currentCaseStatus = currentCaseState.getCaseStatus();

        // Ideally we would like to have different event for changing the readiness reason,
        // but in order to maintain backward compatibility and avoid OPAMI changes we are raising the same
        if (!previousCaseState.equals(currentCaseState)) {
            if (isItValidStatusTransition(previousCaseStatus, currentCaseStatus)) {
                raiseCaseMarkedOrUnmarkedEvent(aggregateState, currentCaseState, previousCaseStatus, streamBuilder);
            } else {
                LOGGER.warn("Invalid transition! The caseStatus was changed from {} to {}", previousCaseStatus, currentCaseStatus);
            }

            raiseCaseStatusChangedWhenTheStatusIsDifferent(aggregateState, streamBuilder, previousCaseStatus,
                    currentCaseStatus);
        }
        return streamBuilder.build();
    }

    private void raiseCaseStatusChangedWhenTheStatusIsDifferent(final CaseAggregateState aggregateState, final Stream.Builder<Object> streamBuilder, final CaseStatus previousCaseStatus, final CaseStatus currentCaseStatus
    ) {
        if (previousCaseStatus != currentCaseStatus) {
            streamBuilder.add(new CaseStatusChanged(aggregateState.getCaseId(), currentCaseStatus));
        }
    }

    private boolean isPreviousStatusReadyAndIsCurrentStatusTerminal(final CaseStatus previousCaseStatus, final CaseStatus currentCaseStatus) {
        return isAReadyStatus(previousCaseStatus) && isTerminalStatus(currentCaseStatus);
    }

    @SuppressWarnings("squid:S1067")
    private boolean isItValidStatusTransition(final CaseStatus previousCaseStatus, final CaseStatus currentCaseStatus) {
        return  wasNotCompletedOrIsValidTransitionFromComplete(previousCaseStatus, currentCaseStatus)
                && wasCompletedOrIsNotValidTransitionFromComplete(previousCaseStatus, currentCaseStatus)
                && wasNotReopenedInLibraOrIsAllowedTransitionFromReopenedInLibra(previousCaseStatus, currentCaseStatus)
                && (previousCaseStatus != REFERRED_FOR_COURT_HEARING) // terminal status, cannot be changed
                && wasReadyOrIsAllowedFromNonReady(previousCaseStatus, currentCaseStatus)
                && !(isPreviousStatusReadyAndIsCurrentStatusTerminal(previousCaseStatus, currentCaseStatus) && previousCaseStatus != COMPLETED_APPLICATION_PENDING);
    }

    private boolean wasReadyOrIsAllowedFromNonReady(final CaseStatus previousCaseStatus, final CaseStatus currentCaseStatus) {
        return isAReadyStatus(previousCaseStatus) || !isNotAllowedFromNonReady(currentCaseStatus);
    }

    private boolean wasNotReopenedInLibraOrIsAllowedTransitionFromReopenedInLibra(final CaseStatus previousCaseStatus, final CaseStatus currentCaseStatus) {
        return previousCaseStatus != REOPENED_IN_LIBRA || isAllowedStatusFromReopenedToLibra(currentCaseStatus);
    }

    private boolean wasCompletedOrIsNotValidTransitionFromComplete(final CaseStatus previousCaseStatus, final CaseStatus currentCaseStatus) {
        return previousCaseStatus == COMPLETED || !isAllowedStatusFromComplete(currentCaseStatus);
    }

    private boolean wasNotCompletedOrIsValidTransitionFromComplete(final CaseStatus previousCaseStatus, final CaseStatus currentCaseStatus) {
        return previousCaseStatus != COMPLETED || isAllowedStatusFromComplete(currentCaseStatus);
    }

    private void raiseCaseMarkedOrUnmarkedEvent(final CaseAggregateState aggregateState, final CaseState currentCaseState,
                                                final CaseStatus previousCaseStatus, final Stream.Builder<Object> streamBuilder) {
        final CaseStatus currentCaseStatus = currentCaseState.getCaseStatus();
        if (isAReadyStatus(currentCaseStatus) && !aggregateState.hasRefusedApplication()) {
            raiseMarkedReadyEvent(aggregateState, currentCaseState, streamBuilder);
        } else {
            checkAndRaiseUnmarkedReadyOrExpectedDateReadyEvent(aggregateState, previousCaseStatus, streamBuilder);
        }
    }

    private void raiseMarkedReadyEvent(final CaseAggregateState aggregateState, final CaseState currentCaseState, final Stream.Builder<Object> streamBuilder) {
        final CaseReadinessReason caseReadinessReason = currentCaseState.getCaseReadinessReason();
        streamBuilder.add(new CaseMarkedReadyForDecision(aggregateState.getCaseId(),
                caseReadinessReason,
                calculateMarkedAtReadyDate(aggregateState),
                getSessionType(caseReadinessReason, aggregateState.isPostConviction(), aggregateState.isSetAside(),aggregateState.hasPendingApplication()),
                getPriority(aggregateState)));
    }

    private void checkAndRaiseUnmarkedReadyOrExpectedDateReadyEvent(final CaseAggregateState aggregateState, final CaseStatus previousCaseStatus, final Stream.Builder<Object> streamBuilder) {
        if (isAReadyStatus(previousCaseStatus)) {
            streamBuilder.add(new CaseUnmarkedReadyForDecision(aggregateState.getCaseId(),
                    expectedDateReadyCalculator.calculateExpectedDateReady(aggregateState)));
        } else {
            final LocalDate oldExpectedDateReady = aggregateState.getExpectedDateReady();
            final LocalDate newExpectedDateReady = expectedDateReadyCalculator.calculateExpectedDateReady(aggregateState);

            if (oldExpectedDateReady != null && !newExpectedDateReady.equals(oldExpectedDateReady)) {
                streamBuilder.add(new CaseExpectedDateReadyChanged(aggregateState.getCaseId(), oldExpectedDateReady, newExpectedDateReady));
            }
        }
    }

    private ZonedDateTime calculateMarkedAtReadyDate(final CaseAggregateState state) {
        return state.isAlreadyMarkedAsReadyForDecision() ? state.getMarkedReadyForDecision() : ZonedDateTime.now();
    }

}
