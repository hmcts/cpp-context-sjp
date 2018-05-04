package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

public class CaseDocumentAddedToCaseDocument implements Converter<CaseDocumentAdded, CaseDocument> {

    @Override
    public CaseDocument convert(CaseDocumentAdded source) {
        uk.gov.moj.cpp.sjp.domain.CaseDocument caseDocument = source.getCaseDocument();

        return new CaseDocument(caseDocument.getId(),
                caseDocument.getMaterialId(),
                caseDocument.getDocumentType(),
                caseDocument.getAddedAt(), source.getCaseId(), source.getIndexWithinDocumentType());
    }
}
