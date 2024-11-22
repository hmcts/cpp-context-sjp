package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAlreadyExists;
import uk.gov.moj.cpp.sjp.event.CaseDocumentDeleted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploadRejected;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;

import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.sjp.event.DeleteCaseDocumentRequestRejected;

public class CaseDocumentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocumentHandler.class);

    public static final CaseDocumentHandler INSTANCE = new CaseDocumentHandler();
    private static final String CASE_IN_SESSION = "CASE_IN_SESSION";

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
                                             final String documentType,
                                             final CaseAggregateState state) {

        if (state.isCaseReferredForCourtHearing()) {
            LOGGER.warn("Case Document Upload rejected as case is referred to court for hearing: {}", documentReference);
            final String description = format("Case Document %s Upload rejected as case %s is referred to court for hearing", documentReference, caseId);
            return Stream.of(new CaseDocumentUploadRejected(documentReference, description));
        }

        if(!state.isManagedByAtcm()) {
            LOGGER.warn("Case Document Upload rejected as case is no longer managed by ATCM: {}", documentReference);
            final String description = format("Case Document %s Upload rejected as case %s is not managed by ATCM", documentReference, caseId);
            return Stream.of(new CaseDocumentUploadRejected(documentReference, description));
        }
        return Stream.of(new CaseDocumentUploaded(caseId, documentReference, documentType));
    }

    public Stream<Object> deleteCaseDocument(final CaseAggregateState caseAggregateState, final UUID documentId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (nonNull(caseAggregateState.getAssigneeId())) {
            streamBuilder.add(new DeleteCaseDocumentRequestRejected(caseAggregateState.getCaseId(), documentId, CASE_IN_SESSION));
        } else {
            final CaseDocument caseDocument = caseAggregateState.getCaseDocuments().get(documentId);

            if(nonNull(caseDocument)){
                streamBuilder.add(new CaseDocumentDeleted(caseAggregateState.getCaseId(), caseDocument));
            } else {
                LOGGER.info("Document={} you are trying to delete may have already been deleted!!", documentId);
            }
        }
        return streamBuilder.build();
    }

}
