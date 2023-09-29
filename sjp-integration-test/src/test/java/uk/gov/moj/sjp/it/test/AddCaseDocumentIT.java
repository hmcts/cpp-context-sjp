package uk.gov.moj.sjp.it.test;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.MaterialStub.stubAddCaseMaterial;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllReferenceData;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAnyQueryOffences;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralReason;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.MaterialStub;
import uk.gov.moj.sjp.it.stub.ProgressionServiceStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

public class AddCaseDocumentIT extends BaseIntegrationTest {

    private static final String PROSECUTING_AUTHORITY_ACCESS_ALL = "ALL";

    private final UUID caseId = randomUUID();
    private final UUID offenceId = randomUUID();
    private final EventListener eventListener = new EventListener();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private static final String DEFENDANT_REGION = "croydon";
    private static final String NATIONAL_COURT_CODE = "1080";

    @Before
    public void setUp() {
        createCasePayloadBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId);

        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);
        stubAllReferenceData();

    }

    private void createCase() {
        createCaseForPayloadBuilder(createCasePayloadBuilder);

    }

    private void createCaseAndWaitUntilReady() {
        new EventListener().subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> createCase())
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);
    }

    private void databaseCleanup() throws Exception {
        new SjpDatabaseCleaner().cleanViewStore();
    }

    @Test
    public void addMultipleCaseDocumentOfSpecificTypeAndVerifySequence() {
        createCase();

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
        createCase();
        stubAnyQueryOffences();

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.addCaseDocument();
            caseDocumentHelper.verifyInActiveMQ();
            caseDocumentHelper.verifyInPublicTopic();
            caseDocumentHelper.assertDocumentAdded();
            caseDocumentHelper.stubGetMetadata();
            caseDocumentHelper.assertDocumentMetadataAvailable();
        }
    }

    @Test
    public void addOtherDocumentAndVerifyNotVisibleForTflUser() {
        createCase();
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
        createCase();
        stubAddCaseMaterial();

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.uploadDocument(documentType);
            final UUID documentId = caseDocumentHelper.verifyCaseDocumentUploadedEventRaised();
            final UUID materialId = MaterialStub.processMaterialAddedCommand(documentId);
            CaseDocumentHelper.assertDocumentAdded(USER_ID, caseId, materialId, documentId, documentType);
        }
    }
    @Test
    public void shouldUploadCaseDocumentApplication() {
        final String documentType = "APPLICATION";
        createCase();
        stubAddCaseMaterial();

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.uploadDocument(documentType);
            final UUID documentId = caseDocumentHelper.verifyCaseDocumentUploadedEventRaised();
            final UUID materialId = MaterialStub.processMaterialAddedCommand(documentId);
            CaseDocumentHelper.assertDocumentAdded(USER_ID, caseId, materialId, documentId, documentType);
        }
    }


    @Test
    public void addCaseDocumentRejectsWhenCaseIsInReferToCourtHearingStatus() throws Exception {
        final UUID referralReasonId = randomUUID();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        databaseCleanup();
        createCaseAndWaitUntilReady();

        final UUID sessionId = randomUUID();
        final UUID prosecutorId = randomUUID();

        final UUID hearingTypeId = randomUUID();
        final String listingNotes = randomAlphanumeric(20);
        final int estimatedHearingDuration = nextInt(1, 999);
        final String hearingCode = "PLE";
        final User legalAdviser = user()
                .withUserId(USER_ID)
                .withFirstName("John")
                .withLastName("Smith")
                .build();

        ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery();
        ReferenceDataServiceStub.stubReferralReasonsQuery(referralReasonId, hearingCode, "");
        stubReferralReason(referralReasonId.toString(), "stub-data/referencedata.referral-reason.json");
        ReferenceDataServiceStub.stubHearingTypesQuery(hearingTypeId.toString(), hearingCode, "");
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        ReferenceDataServiceStub.stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), prosecutorId);
        ReferenceDataServiceStub.stubQueryOffencesByCode(createCasePayloadBuilder.getOffenceBuilder().getLibraOffenceCode());
        ReferenceDataServiceStub.stubReferralDocumentMetadataQuery(randomUUID().toString(), "SJPN");
        SchedulingStub.stubStartSjpSessionCommand();
        ProgressionServiceStub.stubReferCaseToCourtCommand();

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        startSession(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, USER_ID);

        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), false, NO_DISABILITY_NEEDS);
        final ReferForCourtHearing referForCourtHearing = new ReferForCourtHearing(null, asList(new OffenceDecisionInformation(offenceId, VerdictType.PROVED_SJP)), referralReasonId, listingNotes, estimatedHearingDuration, defendantCourtOptions);

        final DecisionCommand decision = new DecisionCommand(sessionId, createCasePayloadBuilder.getId(), null, legalAdviser, asList(referForCourtHearing), null);


        eventListener
                .subscribe(CaseReferredForCourtHearing.EVENT_NAME)
                .subscribe("public.events.hearing.hearing-resulted")
                .run(() -> DecisionHelper.saveDecision(decision))
                .popEvent(CaseReferredForCourtHearing.class.getAnnotation(Event.class).value());

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.uploadPleaCaseDocument();
            caseDocumentHelper.verifyInActiveMQCaseUploadRejected();
            caseDocumentHelper.verifyUploadRejectedInPublicTopic();
        }
    }

    @Test
    public void addsDocumentNumberToDuplicateDocumentTypes() {
        createCase();

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
