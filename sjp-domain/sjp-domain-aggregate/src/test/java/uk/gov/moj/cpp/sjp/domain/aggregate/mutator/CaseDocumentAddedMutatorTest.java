package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CaseDocumentAddedMutatorTest {

    @Test
    public void shouldAddCaseDocument() {
        UUID documentId = UUID.randomUUID();
        String documentType = "pdf";
        CaseDocument caseDocument = new CaseDocument(documentId, UUID.randomUUID(), documentType, ZonedDateTime.now());

        CaseDocumentAdded event = new CaseDocumentAdded(UUID.randomUUID(), caseDocument, 0);
        CaseAggregateState state = new CaseAggregateState();

        CaseDocumentAddedMutator.INSTANCE.apply(event, state);

        assertThat(state.getCaseDocuments().get(documentId), is(caseDocument));
        assertThat(state.getDocumentCountByDocumentType().getCount(documentType), is(1));
    }
}
