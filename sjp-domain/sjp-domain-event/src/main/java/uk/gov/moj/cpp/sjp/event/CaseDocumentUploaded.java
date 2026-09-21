package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("sjp.events.case-document-uploaded")
public class CaseDocumentUploaded {

    private final UUID caseId;

    /**
     * File service id of the document. Null when the document is addressed by
     * {@link #documentReferenceUri} instead - exactly one of the two is set.
     */
    private final UUID documentReference;

    /**
     * Blob uri of the document. Null when the document is addressed by
     * {@link #documentReference} instead - exactly one of the two is set.
     *
     * <p>Absent from events recorded before blob addressing existed; those replay with this field
     * null and {@code documentReference} populated, exactly as before.
     */
    private final String documentReferenceUri;

    private final String documentType;

    @JsonCreator
    public CaseDocumentUploaded(UUID caseId, UUID documentReference, String documentReferenceUri, String documentType) {
        this.caseId = caseId;
        this.documentReference = documentReference;
        this.documentReferenceUri = documentReferenceUri;
        this.documentType = documentType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDocumentReference() {
        return documentReference;
    }

    public String getDocumentReferenceUri() {
        return documentReferenceUri;
    }

    public String getDocumentType() {
        return documentType;
    }
}
