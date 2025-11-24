package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.Test;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentDeleted;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CaseDocumentDeletedMutatorTest {

    @Test
    public void shouldDeleteCaseDocumentWhenNoDocumentsPreviousSaved() {
        UUID documentId = UUID.randomUUID();
        String documentType = "pdf";
        CaseDocument caseDocument = new CaseDocument(documentId, UUID.randomUUID(), documentType, ZonedDateTime.now());

        CaseDocumentDeleted caseDocumentDeletedEvent = new CaseDocumentDeleted(UUID.randomUUID(), caseDocument);
        CaseAggregateState state = new CaseAggregateState();

        CaseDocumentDeletedMutator.INSTANCE.apply(caseDocumentDeletedEvent, state);

        assertThat(state.getCaseDocuments().isEmpty(), is(true));
        assertThat(state.getDocumentCountByDocumentType().getCount(documentType), is(0));
    }

    @Test
    public void shouldDeleteCaseDocument() {
        UUID documentId = UUID.randomUUID();
        String documentType = "pdf";
        CaseDocument caseDocument = new CaseDocument(documentId, UUID.randomUUID(), documentType, ZonedDateTime.now());

        CaseDocumentAdded caseDocumentAddedEvent = new CaseDocumentAdded(UUID.randomUUID(), caseDocument, 0);
        CaseDocumentDeleted caseDocumentDeletedEvent = new CaseDocumentDeleted(UUID.randomUUID(), caseDocument);
        CaseAggregateState state = new CaseAggregateState();

        //add case document
        CaseDocumentAddedMutator.INSTANCE.apply(caseDocumentAddedEvent, state);
        assertThat(state.getCaseDocuments().get(documentId), is(caseDocument));
        assertThat(state.getDocumentCountByDocumentType().getCount(documentType), is(1));

        //delete case document
        CaseDocumentDeletedMutator.INSTANCE.apply(caseDocumentDeletedEvent, state);

        assertThat(state.getCaseDocuments().isEmpty(), is(true));
        assertThat(state.getDocumentCountByDocumentType().getCount(documentType), is(0));
    }
}
