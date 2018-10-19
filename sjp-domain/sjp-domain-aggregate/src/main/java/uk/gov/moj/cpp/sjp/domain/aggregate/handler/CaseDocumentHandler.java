package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAlreadyExists;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;

import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseDocumentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocumentHandler.class);

    public static final CaseDocumentHandler INSTANCE = new CaseDocumentHandler();

    private CaseDocumentHandler() {
    }


    public Stream<Object> addCaseDocument(final UUID caseId,
                                          final CaseDocument caseDocument,
                                          final CaseAggregateState state) {

        if (state.getCaseDocuments().containsKey(caseDocument.getId())) {
            LOGGER.warn("Case Document already exists with ID {}", caseDocument.getId());
            return Stream.of(
                    new CaseDocumentAlreadyExists(caseDocument.getId(), "Add Case Document"));
        }

        final int documentCount = state.getDocumentCountByDocumentType()
                .getCount(caseDocument.getDocumentType());

        return Stream.of(new CaseDocumentAdded(caseId, caseDocument, documentCount + 1));
    }

    public Stream<Object> uploadCaseDocument(final UUID caseId,
                                             final UUID documentReference,
                                             final String documentType) {

        return Stream.of(new CaseDocumentUploaded(caseId, documentReference, documentType));
    }
}
