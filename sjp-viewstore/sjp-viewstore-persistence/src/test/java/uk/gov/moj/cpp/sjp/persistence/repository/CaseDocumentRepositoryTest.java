package uk.gov.moj.cpp.sjp.persistence.repository;

import static junit.framework.TestCase.assertEquals;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseDocumentRepositoryTest extends BaseTransactionalTest {

    @Inject
    private CaseDocumentRepository caseDocumentRepository;

    @Inject
    private CaseRepository caseRepository;

    private ZonedDateTime addedAt_2017_01_01 = ZonedDateTimes.fromString("2017-01-01T00:00:00.000Z");
    private ZonedDateTime addedAt_2017_01_05 = ZonedDateTimes.fromString("2017-01-05T00:00:00.000Z");
    private ZonedDateTime addedAt_2017_01_10 = ZonedDateTimes.fromString("2017-01-10T00:00:00.000Z");

    private static final Integer DOCUMENT_NUMBER = 1;

    @Before
    public void givenCaseDocuments() {
        addCaseDocument(addedAt_2017_01_01);
        addCaseDocument(addedAt_2017_01_05);
        addCaseDocument(addedAt_2017_01_10);
    }

    @Test
    public void findCaseDocuments() {
        //when
        List<CaseDocument> caseDocuments = caseDocumentRepository
                .findCaseDocumentsOrderedByAddedByDescending(
                        addedAt_2017_01_01, addedAt_2017_01_10,
                        CaseDocument.RESULT_ORDER_DOCUMENT_TYPE);

        //then
        assertEquals(2, caseDocuments.size());
        assertEquals(addedAt_2017_01_05, caseDocuments.get(0).getAddedAt());

        final CaseDocument caseDocument = caseDocuments.get(1);
        assertEquals(addedAt_2017_01_01, caseDocument.getAddedAt());
        assertEquals(CaseDocument.RESULT_ORDER_DOCUMENT_TYPE, caseDocument.getDocumentType());
        assertEquals(DOCUMENT_NUMBER, caseDocument.getDocumentNumber());
    }

    private void addCaseDocument(ZonedDateTime addedAt) {

        DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID(), new PersonalDetails(), null, 1);
        CaseDetail caseDetail = new CaseDetail(UUID.randomUUID(), "URN", null, null, null, null, null, defendantDetail, null, LocalDate.now());
        caseDetail.setDefendant(defendantDetail);
        CaseDocument caseDocument = new CaseDocument(UUID.randomUUID(),
                UUID.randomUUID(), CaseDocument.RESULT_ORDER_DOCUMENT_TYPE,
                addedAt, caseDetail.getId(), DOCUMENT_NUMBER);

        caseRepository.save(caseDetail);
        caseDocumentRepository.save(caseDocument);
    }
}
