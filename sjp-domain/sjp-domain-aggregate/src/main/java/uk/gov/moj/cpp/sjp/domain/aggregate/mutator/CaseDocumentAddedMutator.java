package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;

final class CaseDocumentAddedMutator implements AggregateStateMutator<CaseDocumentAdded, CaseAggregateState> {

    static final CaseDocumentAddedMutator INSTANCE = new CaseDocumentAddedMutator();

    private CaseDocumentAddedMutator() {
    }

    @Override
    public void apply(final CaseDocumentAdded event, final CaseAggregateState state) {
        state.addCaseDocument(event.getCaseDocument().getId(), event.getCaseDocument());
        state.getDocumentCountByDocumentType().increaseCount(event.getCaseDocument().getDocumentType());
    }
}
