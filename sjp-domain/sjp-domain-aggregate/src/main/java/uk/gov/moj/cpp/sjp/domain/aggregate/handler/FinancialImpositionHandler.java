package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAdded;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAddedBdf;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionCorrelationIdAdded;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class FinancialImpositionHandler {

    public static final FinancialImpositionHandler INSTANCE = new FinancialImpositionHandler();

    private FinancialImpositionHandler() {
    }

    public Stream<Object> addFinancialImpositionCorrelationId(CaseAggregateState caseAggregateState,
                                                              final UUID defendantId,
                                                              final UUID correlationId) {
        if (caseAggregateState.getDefendantId().equals(defendantId)) {
            return of(new FinancialImpositionCorrelationIdAdded(caseAggregateState.getCaseId(), defendantId, correlationId));
        } else {
            return empty();
        }
    }

    public Stream<Object> addFinancialImpositionAccountNumber(final CaseAggregateState state,
                                                              final UUID correlationId,
                                                              final String accountNumber) {
        final Optional<UUID> defendantId = state.getDefendantForCorrelationId(correlationId);
        if (defendantId.isPresent()) {
            return of(new FinancialImpositionAccountNumberAdded(state.getCaseId(), defendantId.get(), accountNumber));
        } else {
            return empty();
        }
    }

    public Stream<Object> addFinancialImpositionAccountNumberBdf(final CaseAggregateState state,
                                                                 final UUID defendantId,
                                                                 final UUID correlationId,
                                                                 final String accountNumber) {

        if (state.getDefendantId().equals(defendantId)) {
            return of(new FinancialImpositionAccountNumberAddedBdf(state.getCaseId(), defendantId, correlationId, accountNumber));
        } else {
            return empty();
        }
    }
}
