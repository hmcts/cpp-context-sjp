package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;

import java.util.UUID;

@Event("sjp.events.case-document-already-exists")
public class CaseDocumentAlreadyAdded {

    private final UUID caseId;

    private final CaseDocument caseDocument;

    public CaseDocumentAlreadyAdded(UUID caseId, CaseDocument caseDocument) {
        this.caseId = caseId;
        this.caseDocument = caseDocument;
    }

    public CaseDocument getCaseDocument() {
        return caseDocument;
    }

    public UUID getCaseId() {
        return caseId;
    }

}
