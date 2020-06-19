package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Arrays.asList;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsRejected;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsStarted;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class DeleteDocsHandler {

    public static final DeleteDocsHandler INSTANCE = new DeleteDocsHandler();

    private static final List<DecisionType> DELETE_DOCS_DECISION_TYPES = asList(WITHDRAW, DISMISS);

    private DeleteDocsHandler() {
    }

    public Stream<Object> deleteDocs(CaseAggregateState caseAggregateState) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if(caseAggregateState.isDeleteDocsStarted()){
            streamBuilder.add(new FinancialMeansDeleteDocsRejected(caseAggregateState.getCaseId()));
        } else if(offenceDecisionsValidForDeleteDocs(caseAggregateState)){
            streamBuilder.add(new FinancialMeansDeleteDocsStarted(
                    caseAggregateState.getCaseId(),
                    caseAggregateState.getDefendantId()
            ));
        }
        return streamBuilder.build();
    }

    private boolean offenceDecisionsValidForDeleteDocs(final CaseAggregateState caseAggregateState) {
        final Collection<OffenceDecision> offenceDecisions = caseAggregateState.getOffenceDecisions();
        return caseAggregateState.isCaseCompleted() && !offenceDecisions.isEmpty() && offenceDecisions.
                stream()
                .allMatch(offenceDecision -> DELETE_DOCS_DECISION_TYPES.contains(offenceDecision.getType()));
    }
}
