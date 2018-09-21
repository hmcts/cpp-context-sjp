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
        this(caseDocument.getId(), caseDocument.getMaterialId(), caseDocument.getDocumentType(), caseDocument.getDocumentNumber(), caseDocument.getAddedAt());
    }

    public CaseDocumentView(final UUID id, final UUID materialId, final String documentType, final Integer documentNumber, final ZonedDateTime addedAt) {
        this.id = id;
        this.materialId = materialId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.addedAt = addedAt;
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

}