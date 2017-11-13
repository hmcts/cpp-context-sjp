package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;

@Event("sjp.events.case-document-already-exists")
public class CaseDocumentAlreadyAdded {

    private String caseId;

    private CaseDocument caseDocument;

    public CaseDocumentAlreadyAdded(String caseId, CaseDocument caseDocument) {
        this.caseId = caseId;
        this.caseDocument = caseDocument;
    }

    public CaseDocument getCaseDocument() {
        return caseDocument;
    }

    public String getCaseId() {
        return caseId;
    }

}
