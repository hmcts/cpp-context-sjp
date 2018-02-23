package uk.gov.moj.sjp.it.test;

import static uk.gov.moj.sjp.it.stub.MaterialStub.stubAddCaseMaterial;

import uk.gov.moj.sjp.it.helper.AbstractTestHelper;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for Add case document.
 */
public class AddCaseDocumentIT extends BaseIntegrationTest {

    private CaseSjpHelper caseHelper;

    @Before
    public void setUp() {
        caseHelper = new CaseSjpHelper();
        caseHelper.createAndVerifyCase();
    }

    @After
    public void tearDown() {
        caseHelper.close();
    }

    @Test
    public void addMultipleCaseDocumentOfSpecificTypeAndVerifySequence() {
        final UUID legalAdviserId = UUID.randomUUID();
        UsersGroupsStub.stubGroupForUser(legalAdviserId.toString(), UsersGroupsStub.LEGAL_ADVISERS_GROUP);
        stubAddCaseMaterial();

        try (final CaseSjpHelper sjpHelper = new CaseSjpHelper()) {
            sjpHelper.createAndVerifyCase();

            try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(sjpHelper.getCaseId())) {
                caseDocumentHelper.addCaseDocumentWithDocumentType(legalAdviserId, "OTHER-TravelCard");
                caseDocumentHelper.addCaseDocumentWithDocumentType(legalAdviserId, "OTHER-TravelCard");
                caseDocumentHelper.assertDocumentNumber(legalAdviserId, 0, "OTHER-TravelCard", 1);
                caseDocumentHelper.assertDocumentNumber(legalAdviserId, 1, "OTHER-TravelCard", 2);
            }
        }
    }

    @Test
    public void addCaseDocumentWithDocumentFileAndVerifyDocumentAdded() {
        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseHelper.getCaseId())) {
            caseDocumentHelper.addCaseDocument();
            caseDocumentHelper.verifyInActiveMQ();
            caseDocumentHelper.verifyInPublicTopic();
            caseDocumentHelper.assertDocumentAdded();
        }
    }

    @Test
    public void addOtherDocumentAndVerifyNotVisibleForTflUser() {
        String tflUserId = UUID.randomUUID().toString();
        UsersGroupsStub.stubGroupForUser(tflUserId, UsersGroupsStub.TFL_USERS_GROUP);

        String courtAdminUserId = UUID.randomUUID().toString();
        UsersGroupsStub.stubGroupForUser(courtAdminUserId, UsersGroupsStub.COURT_ADMINISTRATORS_GROUP);

        try (CaseSjpHelper sjpHelper = new CaseSjpHelper()) {
            sjpHelper.createAndVerifyCase();

            try (CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(sjpHelper.getCaseId())) {
                caseDocumentHelper.addCaseDocumentWithDocumentType(UUID.fromString(courtAdminUserId), "OTHER");
                caseDocumentHelper.verifyInActiveMQ();
                caseDocumentHelper.assertDocumentAdded(courtAdminUserId);
                caseDocumentHelper.verifyDocumentNotVisibleForProsecutorWhenQueryingForCaseDocuments(tflUserId);
                caseDocumentHelper.verifyDocumentNotVisibleForProsecutorWhenQueryingForACase(tflUserId);
            }
        }
    }

    @Test
    public void shouldUploadPleaCaseDocument() {
        stubAddCaseMaterial();
        try (CaseSjpHelper sjpHelper = new CaseSjpHelper()) {
            sjpHelper.createAndVerifyCase();

            try (CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(sjpHelper.getCaseId())) {
                caseDocumentHelper.uploadPleaCaseDocument();
                final String documentReference = caseDocumentHelper.verifyCaseDocumentUploadedEventRaised();
                caseDocumentHelper.assertCaseMaterialAdded(documentReference);
            }
        }
    }

    @Test
    public void addsDocumentNumberToDuplicateDocumentTypes() {
        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseHelper.getCaseId())) {
            caseDocumentHelper.addCaseDocument();
            caseDocumentHelper.verifyInActiveMQ();
            caseDocumentHelper.verifyInPublicTopic();
            caseDocumentHelper.assertDocumentAdded();
            caseDocumentHelper.assertDocumentNumber(UUID.fromString(AbstractTestHelper.USER_ID), 0, "SJPN", 1);
        }
    }
}
