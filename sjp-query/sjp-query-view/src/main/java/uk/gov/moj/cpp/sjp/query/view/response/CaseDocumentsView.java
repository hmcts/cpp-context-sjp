package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.List;

public class CaseDocumentsView {

    private List<CaseDocumentView> caseDocuments;

    public CaseDocumentsView(List<CaseDocumentView> caseDocuments) {
        this.caseDocuments = caseDocuments;
    }

    public List<CaseDocumentView> getCaseDocuments() {
        return caseDocuments;
    }

    public void setCaseDocuments(List<CaseDocumentView> caseDocuments) {
        this.caseDocuments = caseDocuments;
    }
}
