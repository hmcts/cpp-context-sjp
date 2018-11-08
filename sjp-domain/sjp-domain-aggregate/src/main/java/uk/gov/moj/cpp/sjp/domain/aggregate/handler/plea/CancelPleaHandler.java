package uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.PleaHandlerUtils.changePlea;

import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseLanguageHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;

import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;

public class CancelPleaHandler {

    public static final CancelPleaHandler INSTANCE = new CancelPleaHandler();

    private final CaseLanguageHandler caseLanguageHandler;

    private CancelPleaHandler() {
        caseLanguageHandler = CaseLanguageHandler.INSTANCE;
    }

    @VisibleForTesting
    CancelPleaHandler(CaseLanguageHandler caseLanguageHandler) {
        this.caseLanguageHandler = caseLanguageHandler;
    }

    public Stream<Object> cancelPlea(final UUID userId,
                                     final CancelPlea cancelPleaCommand,
                                     final CaseAggregateState state) {

        return changePlea(
                userId,
                cancelPleaCommand,
                state,
                defendantId -> createCancelledPleaEvent(defendantId, cancelPleaCommand, state));
    }

    private Stream<Object> createCancelledPleaEvent(final UUID defendantId,
                                                    final CancelPlea cancelPlea,
                                                    final CaseAggregateState state) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(new PleaCancelled(cancelPlea.getCaseId(), cancelPlea.getOffenceId(), state.isProvedInAbsence()));

        if (state.isTrialRequested()) {
            streamBuilder.add(new TrialRequestCancelled(state.getCaseId()));
        }

        caseLanguageHandler.updateHearingRequirements(false, null, defendantId, null, null, state)
                .forEach(streamBuilder::add);

        return streamBuilder.build();
    }
}
