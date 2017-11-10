package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDocument implements Serializable {

    private static final long serialVersionUID = 2677950639351341051L;

    private final String id;

    private final String materialId;

    private String documentName;

    private final String documentType;

    private final ZonedDateTime addedAt;

    public CaseDocument(String id, String materialId, String documentType, ZonedDateTime addedAt) {
        this.id = id;
        this.materialId = materialId;
        this.documentType = documentType;
        this.addedAt = addedAt;
    }

    public String getId() {
        return id;
    }

    public String getMaterialId() {
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

}