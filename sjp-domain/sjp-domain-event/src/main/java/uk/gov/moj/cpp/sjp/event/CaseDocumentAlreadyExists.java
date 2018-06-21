package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("sjp.events.case-document-addition-failed")
public class CaseDocumentAlreadyExists {

    private final UUID documentId;
    private final String description;

    public CaseDocumentAlreadyExists(UUID documentId, String description) {
        this.documentId = documentId;
        this.description = description;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getDescription() {
        return description;
    }
}
