package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;

import java.util.*;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;

public final class HandlerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerUtils.class);

    private HandlerUtils() {
    }

    public static Optional<Stream<Object>> createRejectionEvents(final UUID userId,
                                                                 final String action,
                                                                 final UUID defendantId,
                                                                 final CaseAggregateState state) {
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
        }

        return Optional.ofNullable(event).map(Stream::of);
    }

    public static Optional<Stream<Object>> createRejectionEvents(final UUID userId,
                                                                 final CaseAggregateState state,
                                                                 final List<Plea> pleas,
                                                                 final String action){
        Object event = null;

        if(isNull(state.getCaseId())){
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
        } else if (existsPleaWithUnknownOffence(pleas, state)){
            LOGGER.warn("Update rejected because of unknown offence: {}", action);
            event = getPleaWithUnknownOffence(pleas, state).map(plea -> new OffenceNotFound(plea.getOffenceId(), action)).
                    orElse(null);
        } else if(existsPleaWithUnknownDefendant(pleas, state)){
            LOGGER.warn("Update rejected because of unknown defendant: {}", action);
            event = getPleaWithUnknownDefendant(pleas,state).map(plea -> new DefendantNotFound(plea.getDefendantId(), action)).
                    orElse(null);
        } else if (isPleaSubmittedForAnOffenceWithFinalDecision(pleas, state)) {
            LOGGER.warn("plea rejected because final decision is already taken for the offence {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.PLEA_REJECTED_AS_FINAL_DECISION_TAKEN_FOR_OFFENCE);
        }

        return Optional.ofNullable(event).map(Stream::of);
    }

    private static boolean existsPleaWithUnknownOffence(List<Plea> pleas, CaseAggregateState state) {
        return pleas!=null && pleas.stream().anyMatch(plea -> unknownOffenceForCase(plea, state));
    }

    private static boolean existsPleaWithUnknownDefendant(List<Plea> pleas, CaseAggregateState state) {
        return pleas!=null && pleas.stream().anyMatch(plea -> unknownDefendantForCase(plea, state));
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

}
