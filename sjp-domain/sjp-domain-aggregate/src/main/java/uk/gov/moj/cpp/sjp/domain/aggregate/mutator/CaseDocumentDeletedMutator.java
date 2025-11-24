package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseDocumentDeleted;

final class CaseDocumentDeletedMutator implements AggregateStateMutator<CaseDocumentDeleted, CaseAggregateState> {

    static final CaseDocumentDeletedMutator INSTANCE = new CaseDocumentDeletedMutator();

    private CaseDocumentDeletedMutator() {
    }

    @Override
    public void apply(final CaseDocumentDeleted event, final CaseAggregateState state) {
        state.removeCaseDocument(event.getCaseDocument().getId(), event.getCaseDocument());
        state.getDocumentCountByDocumentType().decreaseCount(event.getCaseDocument().getDocumentType());
    }
}
