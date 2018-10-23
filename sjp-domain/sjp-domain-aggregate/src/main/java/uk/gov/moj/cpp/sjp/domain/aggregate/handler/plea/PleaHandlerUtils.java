package uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.command.ChangePlea;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PleaHandlerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleaHandlerUtils.class);

    private PleaHandlerUtils() {
    }

    static Stream<Object> changePlea(
            final UUID userId,
            final ChangePlea changePleaCommand,
            final CaseAggregateState state,
            final Function<UUID, Stream<Object>> changePleaEventCreator) {

        final Optional<UUID> defendantIdOptional = state.getDefendantForOffence(changePleaCommand.getOffenceId());
        if (!defendantIdOptional.isPresent()) {
            final UUID offenceId = changePleaCommand.getOffenceId();
            LOGGER.warn("Cannot change plea for offence which doesn't exist, ID: {}", offenceId);
            return Stream.of(new OffenceNotFound(offenceId, "Update Plea"));
        }

        return createRejectionEvents(
                userId,
                "Change plea",
                null,
                state
        ).orElse(changePleaEventCreator.apply(defendantIdOptional.get()));
    }

    static boolean hasNeverRaisedTrialRequestedEventAndTrialRequired(final UpdatePlea updatePlea,
                                                                     final CaseAggregateState state) {

        return !state.isTrialRequested() && !state.isTrialRequestedPreviously() && trialRequired(updatePlea);
    }

    static boolean wasTrialRequestedThenCancelledAndIsTrialRequiredAgain(final UpdatePlea updatePlea,
                                                                         final CaseAggregateState state) {

        return !state.isTrialRequested() && state.isTrialRequestedPreviously() && trialRequired(updatePlea);
    }

    static boolean isTrialRequestCancellationRequired(final UpdatePlea updatePlea,
                                                      final CaseAggregateState state) {

        return state.isTrialRequested() && !trialRequired(updatePlea);
    }

    private static boolean trialRequired(final UpdatePlea updatePlea) {
        return PleaType.NOT_GUILTY.equals(updatePlea.getPlea());
    }
}
