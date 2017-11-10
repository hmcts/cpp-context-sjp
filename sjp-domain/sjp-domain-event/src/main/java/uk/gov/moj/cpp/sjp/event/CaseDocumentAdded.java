package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;

@Event("structure.events.case-document-added")
public class CaseDocumentAdded {

    private String caseId;

    private CaseDocument caseDocument;

    private int indexWithinDocumentType;

    public CaseDocumentAdded(String caseId, CaseDocument caseDocument, int indexWithinDocumentType) {
        this.caseId = caseId;
        this.caseDocument = caseDocument;
        this.indexWithinDocumentType = indexWithinDocumentType;
    }

    public CaseDocument getCaseDocument() {
        return caseDocument;
    }

    public String getCaseId() {
        return caseId;
    }

    public int getIndexWithinDocumentType() {
        return indexWithinDocumentType;
    }
}
