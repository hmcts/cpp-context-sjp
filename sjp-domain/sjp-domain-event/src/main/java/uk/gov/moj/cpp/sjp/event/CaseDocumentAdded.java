package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;

import java.util.UUID;

@Event(CaseDocumentAdded.EVENT_NAME)
public class CaseDocumentAdded {

    public static final String EVENT_NAME = "sjp.events.case-document-added";

    private UUID caseId;

    private CaseDocument caseDocument;

    private int indexWithinDocumentType;

    public CaseDocumentAdded(UUID caseId, CaseDocument caseDocument, int indexWithinDocumentType) {
        this.caseId = caseId;
        this.caseDocument = caseDocument;
        this.indexWithinDocumentType = indexWithinDocumentType;
    }

    public CaseDocument getCaseDocument() {
        return caseDocument;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public int getIndexWithinDocumentType() {
        return indexWithinDocumentType;
    }
}
