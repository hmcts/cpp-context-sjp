package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("sjp.events.case-document-upload-rejected")
public class CaseDocumentUploadRejected {
    private final UUID documentId;
    private final String description;

    @JsonCreator
    public CaseDocumentUploadRejected(UUID documentId, String description) {
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

