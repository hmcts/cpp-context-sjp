package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;

import java.util.Optional;
import java.util.stream.Stream;

public class CaseWithdrawalHandler {

    public static final CaseWithdrawalHandler INSTANCE = new CaseWithdrawalHandler();

    private CaseWithdrawalHandler() {
    }

    public Stream<Object> requestWithdrawalAllOffences(final CaseAggregateState state) {
        return createRejectionEvents(
                null,
                "Request withdrawal all offences",
                null,
                state
        ).orElse(Stream.of(new AllOffencesWithdrawalRequested(state.getCaseId())));
    }

    public Stream<Object> cancelRequestWithdrawalAllOffences(final CaseAggregateState state) {
        return createRejectionEvents(
                null,
                "Request withdrawal all offences",
                null,
                state
        ).orElse(createOffencesWithdrawalEvent(state)
            .map(Stream::of)
            .orElse(Stream.of()));
    }

    private Optional<Object> createOffencesWithdrawalEvent(final CaseAggregateState state) {
        return Optional.of(state.isWithdrawalAllOffencesRequested())
                .filter(Boolean::booleanValue)
                .map(allOffencesRequested ->
                        new AllOffencesWithdrawalRequestCancelled(state.getCaseId()));
    }
}
