package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;

import java.util.UUID;

@Event(CaseDocumentDeleted.EVENT_NAME)
public class CaseDocumentDeleted {

    public static final String EVENT_NAME = "sjp.events.case-document-deleted";

    private UUID caseId;

    private CaseDocument caseDocument;

    public CaseDocumentDeleted(UUID caseId, CaseDocument caseDocument) {
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
