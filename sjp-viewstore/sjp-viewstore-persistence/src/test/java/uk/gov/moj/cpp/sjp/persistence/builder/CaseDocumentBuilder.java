package uk.gov.moj.cpp.sjp.persistence.builder;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CaseDocumentBuilder {

    private UUID id;
    private UUID materialId;
    private String documentType;

    private CaseDocumentBuilder() {
        this.id = UUID.randomUUID();
        this.materialId = UUID.randomUUID();
        this.documentType = "Default document type";
    }

    public static CaseDocumentBuilder aCaseDocument() {
        return new CaseDocumentBuilder();
    }

    public CaseDocumentBuilder withMaterialId(UUID materialId) {
        this.materialId = materialId;
        return this;
    }

    public CaseDocumentBuilder withDocumentType(String documentType) {
        this.documentType = documentType;
        return this;
    }

    public CaseDocument build() {
        return new CaseDocument(id, materialId, documentType, ZonedDateTime.now(), UUID.randomUUID(), 1);
    }

}
