package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.UUID;

public class CaseDocumentView {

    private UUID id;
    private UUID materialId;
    private String documentType;
    private Integer documentNumber;
    private ZonedDateTime addedAt;

    /**
     * Blob uri the document was filed from, or null when it was addressed by its file service id.
     *
     * <p>For a file-service document {@code id} is the file id; for a blob-addressed one {@code id}
     * is a uuid derived from the uri and this field carries the real reference, so any response
     * exposing {@code id} has to expose this too. Jackson serialises with NON_ABSENT, so the field
     * is simply absent on the file-service path and those responses are unchanged.
     */
    private String documentUri;

    public static final Comparator<CaseDocumentView> BY_DOCUMENT_TYPE_AND_NUMBER = (first, second) -> {
        if (first.documentType == null) {
            return 0;
        }

        int compareDocumentType = first.documentType.compareTo(second.documentType);
        if (compareDocumentType != 0) {
            return compareDocumentType;
        }
        return first.documentNumber.compareTo(second.documentNumber);
    };

    public CaseDocumentView(final CaseDocument caseDocument) {
        this(caseDocument.getId(), caseDocument.getMaterialId(), caseDocument.getDocumentType(), caseDocument.getDocumentNumber(), caseDocument.getAddedAt(), caseDocument.getDocumentUri());
    }

    public CaseDocumentView(final UUID id, final UUID materialId, final String documentType, final Integer documentNumber, final ZonedDateTime addedAt) {
        this(id, materialId, documentType, documentNumber, addedAt, null);
    }

    public CaseDocumentView(final UUID id, final UUID materialId, final String documentType, final Integer documentNumber, final ZonedDateTime addedAt, final String documentUri) {
        this.id = id;
        this.materialId = materialId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.addedAt = addedAt;
        this.documentUri = documentUri;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Integer getDocumentNumber() {
        return documentNumber;
    }

    public ZonedDateTime getAddedAt() {
        return addedAt;
    }

    public String getDocumentUri() {
        return documentUri;
    }

}