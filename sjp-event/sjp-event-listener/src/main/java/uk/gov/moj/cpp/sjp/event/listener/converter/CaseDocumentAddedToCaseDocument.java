package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import java.util.UUID;

public class CaseDocumentAddedToCaseDocument implements Converter<CaseDocumentAdded, CaseDocument> {

    @Override
    public CaseDocument convert(CaseDocumentAdded source) {
        uk.gov.moj.cpp.sjp.domain.CaseDocument caseDocument = source.getCaseDocument();

        return new CaseDocument(UUID.fromString(caseDocument.getId()),
                UUID.fromString(caseDocument.getMaterialId()),
                caseDocument.getDocumentType(),
                caseDocument.getAddedAt(), UUID.fromString(source.getCaseId()), source.getIndexWithinDocumentType());
    }
}
