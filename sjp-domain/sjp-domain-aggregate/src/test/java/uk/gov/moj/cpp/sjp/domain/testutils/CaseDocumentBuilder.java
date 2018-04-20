package uk.gov.moj.cpp.sjp.domain.testutils;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CaseDocumentBuilder {

    private static final Clock clock = new UtcClock();

    private UUID id = DefaultTestData.CASE_DOCUMENT_ID;
    private String materialId = DefaultTestData.CASE_DOCUMENT_MATERIAL_ID;
    private String documentType;

    public static CaseDocument defaultCaseDocument(UUID documentId, String documentType) {
        return new CaseDocument(
                documentId.toString(),
                DefaultTestData.CASE_DOCUMENT_MATERIAL_ID,
                documentType,
                clock.now()
        );
    }

    public static CaseDocument defaultCaseDocument() {
        return new CaseDocument(
                DefaultTestData.CASE_DOCUMENT_ID_STR,
                DefaultTestData.CASE_DOCUMENT_MATERIAL_ID,
                DefaultTestData.CASE_DOCUMENT_TYPE_SJPN,
                clock.now()
        );
    }

    private CaseDocumentBuilder() {
    }

    public static CaseDocumentBuilder aCaseDocument() {
        return new CaseDocumentBuilder();
    }

    public CaseDocument build() {
        return new CaseDocument(id.toString(), materialId, documentType, clock.now());
    }
}
