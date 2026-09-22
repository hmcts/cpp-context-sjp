package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("sjp.events.case-document-addition-failed")
public class CaseDocumentAlreadyExists {

    /**
     * Case the document was being filed against. Always populated for events raised from this
     * version onward; absent from events recorded before it, which replay with this field null.
     */
    private final UUID caseId;

    private final UUID documentId;

    /**
     * Blob uri the rejected document was filed from, or null when it was addressed by its file
     * service id alone. Unlike {@link CaseDocumentUploadRejected}, this is an optional extra rather
     * than an alternative: {@code documentId} is always populated here, because a blob-addressed
     * document's id is derived from its uri before the command is sent.
     *
     * <p>Absent from events recorded before blob addressing existed; those replay with this field
     * null, exactly as before.
     */
    private final String documentUri;

    private final String description;

    @JsonCreator
    public CaseDocumentAlreadyExists(UUID caseId, UUID documentId, String documentUri, String description) {
        this.caseId = caseId;
        this.documentId = documentId;
        this.documentUri = documentUri;
        this.description = description;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getDocumentUri() {
        return documentUri;
    }

    public String getDescription() {
        return description;
    }
}
