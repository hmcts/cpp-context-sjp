package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;

    private final UUID materialId;

    private String documentName;

    private final String documentType;

    private final ZonedDateTime addedAt;

    /**
     * Blob uri the document was filed from, or null when it was addressed by its file service
     * {@link #id} instead.
     *
     * <p>Absent from events recorded before blob addressing existed; those replay with this field
     * null, exactly as before.
     */
    private final String documentUri;

    public CaseDocument(UUID id, UUID materialId, String documentType, ZonedDateTime addedAt, String documentUri) {
        this.id = id;
        this.materialId = materialId;
        this.documentType = documentType;
        this.addedAt = addedAt;
        this.documentUri = documentUri;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentName() {
        return documentName;
    }

    public ZonedDateTime getAddedAt() {
        return addedAt;
    }

    public String getDocumentUri() {
        return documentUri;
    }

}