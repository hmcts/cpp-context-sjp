package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.sjp.event.ProsecutionAuthorityAccessDenied;

public final class HandlerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerUtils.class);

    private HandlerUtils() {
    }

    @SuppressWarnings("squid:S1192")
    public static Optional<Stream<Object>> createRejectionEvents(final UUID userId,
                                                                 final String action,
                                                                 final UUID defendantId,
                                                                 final CaseAggregateState state,
                                                                 final String userProsecutingAuthority,
                                                                 final List<String> agentProsecutorAuthorityAccess) {
        Object event = null;
        if (isNull(state.getCaseId())) {
            LOGGER.warn("Case not found: {}", action);
            event = new CaseNotFound(null, action);
        } else if (nonNull(defendantId) && !state.hasDefendant(defendantId)) {
            LOGGER.warn("Defendant not found: {}", action);
            event = new DefendantNotFound(defendantId, action);
        } else if (nonNull(state.getAssigneeId()) && !state.isAssignee(userId)) {
            LOGGER.warn("Update rejected because case is assigned to another user: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_ASSIGNED);
        } else if (state.isCaseCompleted()) {
            LOGGER.warn("Update rejected because case is already completed: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_COMPLETED);
        } else if (state.isCaseReferredForCourtHearing()) {
            LOGGER.warn("Update rejected because case is referred to court for hearing: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_REFERRED_FOR_COURT_HEARING);
        } else if (!(state.getProsecutingAuthority().toUpperCase().startsWith(userProsecutingAuthority) ||
                "ALL".equalsIgnoreCase(userProsecutingAuthority) ||
                (agentProsecutorAuthorityAccess != null && agentProsecutorAuthorityAccess.stream().anyMatch(s-> s.equalsIgnoreCase(state.getProsecutingAuthority()))))) {
            event = new ProsecutionAuthorityAccessDenied(userProsecutingAuthority, state.getProsecutingAuthority(), agentProsecutorAuthorityAccess);
        }

        return Optional.ofNullable(event).map(Stream::of);
    }
    public static Optional<Stream<Object>> createRejectionEvents(final UUID userId,
                                                                 final String action,
                                                                 final UUID defendantId,
                                                                 final CaseAggregateState state) {
        return createRejectionEvents(userId,action,defendantId,state,false);
    }

    public static Optional<Stream<Object>> createRejectionEvents(final UUID userId,
                                                                 final String action,
                                                                 final UUID defendantId,
                                                                 final CaseAggregateState state,
                                                                 final boolean isAddressUpdateFromApplication) {
        Object event = null;
        if (isNull(state.getCaseId())) {
            LOGGER.warn("Case not found: {}", action);
            event = new CaseNotFound(null, action);
        } else if (nonNull(defendantId) && !state.hasDefendant(defendantId)) {
            LOGGER.warn("Defendant not found: {}", action);
            event = new DefendantNotFound(defendantId, action);
        } else if (nonNull(state.getAssigneeId()) && !state.isAssignee(userId)) {
            LOGGER.warn("Update rejected because case is assigned to another user: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_ASSIGNED);
        } else if (state.isCaseCompleted() && !isAddressUpdateFromApplication) {
            LOGGER.warn("Update rejected because case is already completed: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_COMPLETED);
        } else if (state.isCaseReferredForCourtHearing()) {
            LOGGER.warn("Update rejected because case is referred to court for hearing: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_REFERRED_FOR_COURT_HEARING);
        }

        return Optional.ofNullable(event).map(Stream::of);
    }

    public static Optional<Stream<Object>> createRejectionEvents(final UUID userId,
                                                                 final CaseAggregateState state,
                                                                 final List<Plea> pleas,
                                                                 final String action) {
        Object event = null;

        if (isNull(state.getCaseId())) {
            LOGGER.warn("Case not found: {}", action);
            event = new CaseNotFound(null, action);
        } else if (state.isCaseCompleted()) {
            LOGGER.warn("Update rejected because case is already completed: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_COMPLETED);
        } else if (nonNull(state.getAssigneeId()) && !state.isAssignee(userId)) {
            LOGGER.warn("Update rejected because case is assigned to another user: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_ASSIGNED);
        } else if (state.isCaseReferredForCourtHearing()) {
            LOGGER.warn("Update rejected because case is referred to court for hearing: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_REFERRED_FOR_COURT_HEARING);
        } else if (existsPleaWithUnknownOffence(pleas, state)) {
            LOGGER.warn("Update rejected because of unknown offence: {}", action);
            event = getPleaWithUnknownOffence(pleas, state).map(plea -> new OffenceNotFound(plea.getOffenceId(), action)).
                    orElse(null);
        } else if (existsPleaWithUnknownDefendant(pleas, state)) {
            LOGGER.warn("Update rejected because of unknown defendant: {}", action);
            event = getPleaWithUnknownDefendant(pleas, state).map(plea -> new DefendantNotFound(plea.getDefendantId(), action)).
                    orElse(null);
        } else if (isPleaSubmittedForAnOffenceWithFinalDecision(pleas, state)) {
            LOGGER.warn("plea rejected because final decision is already taken for the offence {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.PLEA_REJECTED_AS_FINAL_DECISION_TAKEN_FOR_OFFENCE);
        } else if (isPleaSubmittedForAnOffenceWithPreviousAdjournPostConviction(state)) {
            LOGGER.warn("plea rejected because the offence {} has a conviction", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.OFFENCE_HAS_CONVICTION);
        }

        return Optional.ofNullable(event).map(Stream::of);
    }

    private static boolean existsPleaWithUnknownOffence(List<Plea> pleas, CaseAggregateState state) {
        return pleas != null && pleas.stream().anyMatch(plea -> unknownOffenceForCase(plea, state));
    }

    private static boolean existsPleaWithUnknownDefendant(List<Plea> pleas, CaseAggregateState state) {
        return pleas != null && pleas.stream().anyMatch(plea -> unknownDefendantForCase(plea, state));
    }

    private static Optional<Plea> getPleaWithUnknownOffence(List<Plea> pleas, CaseAggregateState state) {
        return pleas.stream().filter(plea -> unknownOffenceForCase(plea, state)).findFirst();
    }

    private static Optional<Plea> getPleaWithUnknownDefendant(List<Plea> pleas, CaseAggregateState state) {
        return pleas.stream().filter(plea -> unknownDefendantForCase(plea, state)).findFirst();
    }

    private static boolean unknownOffenceForCase(Plea plea, CaseAggregateState state) {
        return !state.offenceExists(plea.getOffenceId());
    }


    private static boolean unknownDefendantForCase(Plea plea, CaseAggregateState state) {
        return !state.hasDefendant(plea.getDefendantId());
    }

    private static boolean isPleaSubmittedForAnOffenceWithFinalDecision(final List<Plea> newPleaRequests, final CaseAggregateState state) {

        if (caseIsSetAsideDueToApplication(state)) {
            return false;
        }

        final Map<UUID, OffenceDecision> offenceDecisionMap = state.getOffenceDecisionsWithOffenceIds();
        final Collection<UUID> offenceDecisionIds = offenceDecisionMap.keySet();
        for (final UUID offenceDecisionId : offenceDecisionIds) {
            final OffenceDecision offenceDecision = offenceDecisionMap.get(offenceDecisionId);
            if (offenceDecision.isFinalDecision()) {
                final Plea newPlea = getMatchingPlea(offenceDecisionId, newPleaRequests);
                final Plea existingPlea = getMatchingPlea(offenceDecisionId, state.getPleas());
                if ((newPlea != null && existingPlea == null) || (newPlea != null && newPlea.getPleaType() != existingPlea.getPleaType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Plea getMatchingPlea(final UUID offenceId, final Collection<Plea> pleas) {
        return pleas.stream()
                .filter(plea -> plea.getOffenceId().equals(offenceId))
                .findAny()
                .orElse(null);
    }

    private static boolean isPleaSubmittedForAnOffenceWithPreviousAdjournPostConviction(final CaseAggregateState state) {
        if (caseIsSetAsideDueToApplication(state)) {
            return false;
        }

        return state.getOffenceDecisionsWithOffenceIds().entrySet().stream()
                .anyMatch(entry -> DecisionType.ADJOURN == entry.getValue().getType() &&
                        state.offenceHasPreviousConviction(entry.getKey()));
    }

    private static boolean caseIsSetAsideDueToApplication(final CaseAggregateState state) {
        return state.isSetAside() && state.hasGrantedApplication();
    }

    /**
     * Creates rejection events for updates from Criminal Courts (CC).
     * This method only checks for basic validation (case/defendant existence and assignment),
     * but does NOT check for case completed or case referred for court hearing.
     * Returns no event (empty Optional) if case is not found.
     */
    public static Optional<Stream<Object>> createRejectionEventsForDefendantUpdate(
                                                                       final String action,
                                                                       final UUID defendantId,
                                                                       final CaseAggregateState state) {
        if (isNull(state.getCaseId())) {
            LOGGER.warn("Case not found: {}, returning no event", action);
            return Optional.empty();
        }

        Object event = null;
        if (nonNull(defendantId) && !state.hasDefendant(defendantId)) {
            LOGGER.warn("Defendant not found: {}", action);
            event = new DefendantNotFound(defendantId, action);
        }

        return Optional.ofNullable(event).map(Stream::of);
    }
}
