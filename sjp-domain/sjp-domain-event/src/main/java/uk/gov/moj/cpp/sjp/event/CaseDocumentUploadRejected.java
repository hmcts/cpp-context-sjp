package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("sjp.events.case-document-upload-rejected")
public class CaseDocumentUploadRejected {
    /**
     * File service id of the rejected document. Null when it was addressed by
     * {@link #documentReferenceUri} instead - exactly one of the two is set.
     */
    private final UUID documentId;

    /**
     * Blob uri of the rejected document. Null when it was addressed by {@link #documentId}
     * instead - exactly one of the two is set.
     *
     * <p>Absent from events recorded before blob addressing existed; those replay with this field
     * null and {@code documentId} populated, exactly as before.
     */
    private final String documentReferenceUri;

    private final String description;

    @JsonCreator
    public CaseDocumentUploadRejected(UUID documentId, String documentReferenceUri, String description) {
        this.documentId = documentId;
        this.documentReferenceUri = documentReferenceUri;
        this.description = description;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getDocumentReferenceUri() {
        return documentReferenceUri;
    }

    public String getDescription() {
        return description;
    }
}

