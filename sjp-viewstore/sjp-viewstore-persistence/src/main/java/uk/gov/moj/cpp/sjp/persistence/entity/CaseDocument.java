package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "case_document", uniqueConstraints = @UniqueConstraint(columnNames = "id"))
public class CaseDocument implements Serializable {

    private static final long serialVersionUID = -1581766510152881247L;

    public static final String RESULT_ORDER_DOCUMENT_TYPE = "RESULT_ORDER";

    @Column(name = "id", nullable = false, unique = true, length = 2147483647)
    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "material_id", length = 2147483647)
    private UUID materialId;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "added_at")
    private ZonedDateTime addedAt;

    @Column(name = "document_number")
    private Integer documentNumber;

    /**
     * Blob uri the document was filed from, or null when it was addressed by its file service id.
     */
    @Column(name = "document_uri")
    private String documentUri;

    public CaseDocument() {
        super();
    }

    public CaseDocument(UUID id, UUID materialId, String documentType, ZonedDateTime addedAt, UUID caseId, Integer documentNumber, String documentUri) {
        super();
        this.id = id;
        this.materialId = materialId;
        this.documentType = documentType;
        this.addedAt = addedAt;
        this.caseId = caseId;
        this.documentNumber = documentNumber;
        this.documentUri = documentUri;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(UUID materialId) {
        this.materialId = materialId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public ZonedDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(ZonedDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public Integer getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(Integer documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDocumentUri() {
        return documentUri;
    }
    public void setDocumentUri(String documentUri) {
        this.documentUri = documentUri;
    }

}
