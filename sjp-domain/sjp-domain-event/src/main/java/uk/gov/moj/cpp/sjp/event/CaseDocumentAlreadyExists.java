package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

@Event("sjp.events.case-document-addition-failed")
public class CaseDocumentAlreadyExists {

    private final String documentId;
    private final String description;

    public CaseDocumentAlreadyExists(String documentId, String description) {
        this.documentId = documentId;
        this.description = description;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDescription() {
        return description;
    }
}
