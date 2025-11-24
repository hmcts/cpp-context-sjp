package uk.gov.moj.cpp.sjp.persistence.builder;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import java.util.UUID;

public class CaseDocumentBuilder {

    private static final Clock clock = new UtcClock();

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
        return new CaseDocument(id, materialId, documentType, clock.now(), UUID.randomUUID(), 1);
    }

}
