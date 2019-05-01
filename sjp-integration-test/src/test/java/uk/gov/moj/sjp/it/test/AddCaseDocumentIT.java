package uk.gov.moj.sjp.it.test;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_REFERRED_FOR_COURT_HEARING;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.stub.MaterialStub.stubAddCaseMaterial;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.producer.DecisionToReferCaseForCourtHearingSavedProducer;
import uk.gov.moj.sjp.it.stub.MaterialStub;
import uk.gov.moj.sjp.it.stub.ProgressionServiceStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.ResultingStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;

import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;


/**
 * Integration test for Add case document.
 */
public class AddCaseDocumentIT extends BaseIntegrationTest {

    private static final String PROSECUTING_AUTHORITY_ACCESS_ALL = "ALL";

    private final UUID caseId = randomUUID();

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);
        createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

    @Test
    public void addMultipleCaseDocumentOfSpecificTypeAndVerifySequence() {
        final UUID legalAdviserId = randomUUID();
        UsersGroupsStub.stubGroupForUser(legalAdviserId, UsersGroupsStub.LEGAL_ADVISERS_GROUP);
        UsersGroupsStub.stubForUserDetails(legalAdviserId, PROSECUTING_AUTHORITY_ACCESS_ALL);

        stubAddCaseMaterial();

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.addCaseDocumentWithDocumentType(legalAdviserId, "OTHER-TravelCard");
            caseDocumentHelper.addCaseDocumentWithDocumentType(legalAdviserId, "OTHER-TravelCard");
            caseDocumentHelper.findDocument(legalAdviserId, 0, "OTHER-TravelCard", 1);
            caseDocumentHelper.findDocument(legalAdviserId, 1, "OTHER-TravelCard", 2);
        }
    }

    @Test
    public void addCaseDocumentWithDocumentFileAndVerifyDocumentAdded() {
        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.addCaseDocument();
            caseDocumentHelper.verifyInActiveMQ();
            caseDocumentHelper.verifyInPublicTopic();
            caseDocumentHelper.assertDocumentAdded();
        }
    }

    @Test
    public void addOtherDocumentAndVerifyNotVisibleForTflUser() {
        final UUID tflUserId = randomUUID();
        UsersGroupsStub.stubGroupForUser(tflUserId, UsersGroupsStub.SJP_PROSECUTORS_GROUP);
        UsersGroupsStub.stubForUserDetails(tflUserId, ProsecutingAuthority.TFL);

        final UUID courtAdminUserId = randomUUID();
        UsersGroupsStub.stubGroupForUser(courtAdminUserId, UsersGroupsStub.COURT_ADMINISTRATORS_GROUP);
        UsersGroupsStub.stubForUserDetails(courtAdminUserId, PROSECUTING_AUTHORITY_ACCESS_ALL);

        try (CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.addCaseDocumentWithDocumentType(courtAdminUserId, "OTHER");
            caseDocumentHelper.verifyInActiveMQ();
            caseDocumentHelper.assertDocumentAdded(courtAdminUserId);
            caseDocumentHelper.verifyDocumentNotVisibleForProsecutorWhenQueryingForCaseDocuments(tflUserId);
            caseDocumentHelper.verifyDocumentNotVisibleForProsecutorWhenQueryingForACase(tflUserId);
        }
    }

    @Test
    public void shouldUploadCaseDocument() {
        final String documentType = "PLEA";

        stubAddCaseMaterial();

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.uploadDocument(documentType);
            final UUID documentId = caseDocumentHelper.verifyCaseDocumentUploadedEventRaised();
            final UUID materialId = MaterialStub.processMaterialAddedCommand(documentId);
            CaseDocumentHelper.assertDocumentAdded(USER_ID, caseId, materialId, documentId, documentType);
        }
    }

    @Test
    public void addCaseDocumentRejectsWhenCaseIsInReferToCourtHearingStatus() {
        final UUID sessionId = randomUUID();
        final UUID prosecutorId = randomUUID();
        final UUID referralReasonId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final String courtHouseOUCode = "B01OK";
        final String listingNotes = RandomStringUtils.randomAlphanumeric(20);
        final int estimatedHearingDuration = RandomUtils.nextInt(1, 999);

        ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery(courtHouseOUCode, "2572");
        ReferenceDataServiceStub.stubReferralReasonsQuery(referralReasonId.toString(), "");
        ReferenceDataServiceStub.stubHearingTypesQuery(hearingTypeId.toString(), "");
        ReferenceDataServiceStub.stubProsecutorQuery(createCasePayloadBuilder.getProsecutingAuthority().name(), prosecutorId);
        ReferenceDataServiceStub.stubQueryOffences("stub-data/referencedata.query.offences.json");
        ReferenceDataServiceStub.stubReferralDocumentMetadataQuery(randomUUID().toString(), "SJPN");
        ResultingStub.stubGetCaseDecisionsWithDecision(caseId);
        SchedulingStub.stubStartSjpSessionCommand();
        ProgressionServiceStub.stubReferCaseToCourtCommand();

        final DecisionToReferCaseForCourtHearingSavedProducer decisionToReferCaseForCourtHearingSavedProducer = new DecisionToReferCaseForCourtHearingSavedProducer(
                caseId,
                sessionId,
                referralReasonId,
                hearingTypeId,
                estimatedHearingDuration,
                listingNotes,
                now());

        startSession(sessionId, USER_ID, courtHouseOUCode, MAGISTRATE);

        new EventListener()
                .subscribe(EVENT_CASE_REFERRED_FOR_COURT_HEARING)
                .run(decisionToReferCaseForCourtHearingSavedProducer::saveDecisionToReferCaseForCourtHearing)
                .popEvent(EVENT_CASE_REFERRED_FOR_COURT_HEARING);

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.uploadPleaCaseDocument();
            caseDocumentHelper.verifyInActiveMQCaseUploadRejected();
        }
    }

    @Test
    public void addsDocumentNumberToDuplicateDocumentTypes() {
        final UUID userId = randomUUID();
        UsersGroupsStub.stubForUserDetails(userId, PROSECUTING_AUTHORITY_ACCESS_ALL);

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.addCaseDocument();
            caseDocumentHelper.verifyInActiveMQ();
            caseDocumentHelper.verifyInPublicTopic();
            caseDocumentHelper.assertDocumentAdded(userId);
            caseDocumentHelper.findDocument(userId, 0, "SJPN", 1);
        }
    }
}
