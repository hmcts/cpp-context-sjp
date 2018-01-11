package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import java.util.Comparator;
import java.util.UUID;

public class CaseDocumentView {

    private UUID id;
    private UUID materialId;
    private String documentType;
    private Integer documentNumber;

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

    public CaseDocumentView(CaseDocument caseDocument) {
        this.id = caseDocument.getId();
        this.materialId = caseDocument.getMaterialId();
        this.documentType = caseDocument.getDocumentType();
        this.documentNumber = caseDocument.getDocumentNumber();
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

}