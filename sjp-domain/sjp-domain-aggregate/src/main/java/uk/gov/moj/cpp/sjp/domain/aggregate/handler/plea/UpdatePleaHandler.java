package uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.PleaHandlerUtils.changePlea;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.PleaHandlerUtils.hasNeverRaisedTrialRequestedEventAndTrialRequired;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.PleaHandlerUtils.isTrialRequestCancellationRequired;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.PleaHandlerUtils.wasTrialRequestedThenCancelledAndIsTrialRequiredAgain;

import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseLanguageHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;

public class UpdatePleaHandler {

    public static final UpdatePleaHandler INSTANCE = new UpdatePleaHandler();

    private final CaseLanguageHandler caseLanguageHandler;

    private UpdatePleaHandler() {
        caseLanguageHandler = CaseLanguageHandler.INSTANCE;
    }

    @VisibleForTesting
    UpdatePleaHandler(CaseLanguageHandler caseLanguageHandler) {
        this.caseLanguageHandler = caseLanguageHandler;
    }

    public Stream<Object> updatePlea(final UUID userId,
                                     final UpdatePlea updatePleaCommand,
                                     final ZonedDateTime updatedOn,
                                     final CaseAggregateState state) {

        return changePlea(
                userId,
                updatePleaCommand,
                state,
                defendantId -> createUpdatedPleaEvent(defendantId, updatePleaCommand, updatedOn, state));
    }

    private Stream<Object> createUpdatedPleaEvent(final UUID defendantId,
                                                  final UpdatePlea updatePleaCommand,
                                                  final ZonedDateTime updatedOn,
                                                  final CaseAggregateState state) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(
                new PleaUpdated(
                        updatePleaCommand.getCaseId(),
                        updatePleaCommand.getOffenceId(),
                        updatePleaCommand.getPlea(),
                        PleaMethod.POSTAL));

        handleTrialRequestEventsForUpdatePlea(updatePleaCommand, streamBuilder, updatedOn, state);
        caseLanguageHandler
                .updateHearingRequirements(
                        false,
                        null,
                        defendantId,
                        updatePleaCommand.getInterpreterLanguage(),
                        updatePleaCommand.getSpeakWelsh(),
                        state)
                .forEach(streamBuilder::add);

        return streamBuilder.build();
    }

    private void handleTrialRequestEventsForUpdatePlea(final UpdatePlea updatePlea,
                                                       final Stream.Builder<Object> streamBuilder,
                                                       final ZonedDateTime updatedOn,
                                                       final CaseAggregateState state) {

        if (hasNeverRaisedTrialRequestedEventAndTrialRequired(updatePlea, state)) {
            streamBuilder.add(new TrialRequested(state.getCaseId(), updatedOn));
        } else if (isTrialRequestCancellationRequired(updatePlea, state)) {
            streamBuilder.add(new TrialRequestCancelled(state.getCaseId()));
        } else if (wasTrialRequestedThenCancelledAndIsTrialRequiredAgain(updatePlea, state)) {
            streamBuilder.add(
                    new TrialRequested(
                            state.getCaseId(),
                            state.getTrialRequestedUnavailability(),
                            state.getTrialRequestedWitnessDetails(),
                            state.getTrialRequestedWitnessDispute(),
                            updatedOn));
        }
    }

}
