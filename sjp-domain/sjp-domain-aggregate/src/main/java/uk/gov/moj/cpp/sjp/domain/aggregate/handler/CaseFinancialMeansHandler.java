package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;

import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;

import java.util.UUID;
import java.util.stream.Stream;

public class CaseFinancialMeansHandler {

    public static final CaseFinancialMeansHandler INSTANCE = new CaseFinancialMeansHandler();

    private CaseFinancialMeansHandler() {
    }

    public Stream<Object> updateFinancialMeans(final UUID userId,
                                               final FinancialMeans financialMeans,
                                               final CaseAggregateState state) {

        return createRejectionEvents(
                userId,
                "Update financial means",
                financialMeans.getDefendantId(),
                state
        ).orElse(
                Stream.of(FinancialMeansUpdated.createEvent(
                        financialMeans.getDefendantId(),
                        financialMeans.getIncome(),
                        financialMeans.getBenefits(),
                        financialMeans.getEmploymentStatus())));
    }
}
