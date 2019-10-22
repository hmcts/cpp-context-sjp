package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;

import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleted;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;

import java.util.List;
import java.util.Map;
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

    public Stream<Object> deleteFinancialMeans(final UUID defendantId, final CaseAggregateState state) {

        final List<UUID> materialIds = state.getCaseDocuments()
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .filter(caseDocument ->
                        CaseAggregateState.FINANCIAL_MEANS_DOCUMENT_TYPE
                                .equals(caseDocument.getDocumentType()))
                .map(CaseDocument::getMaterialId)
                .collect(toList());

        return Stream.of(FinancialMeansDeleted.createEvent(defendantId, materialIds));
    }
}
