package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_REFER_FOR_COURT_HEARING_IN_RESULTING;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.CaseDocumentHelper.sendPublicEvent;
import static uk.gov.moj.sjp.it.stub.MaterialStub.stubAddCaseMaterial;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;


/**
 * Integration test for Add case document.
 */
public class AddCaseDocumentIT extends BaseIntegrationTest {

    private static final String PROSECUTING_AUTHORITY_ACCESS_ALL = "ALL";

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

    @Test
    public void addMultipleCaseDocumentOfSpecificTypeAndVerifySequence() {
        final UUID legalAdviserId = UUID.randomUUID();
        UsersGroupsStub.stubGroupForUser(legalAdviserId, UsersGroupsStub.LEGAL_ADVISERS_GROUP);
        UsersGroupsStub.stubForUserDetails(legalAdviserId, PROSECUTING_AUTHORITY_ACCESS_ALL);

        stubAddCaseMaterial();

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(createCasePayloadBuilder.getId())) {
            caseDocumentHelper.addCaseDocumentWithDocumentType(legalAdviserId, "OTHER-TravelCard");
            caseDocumentHelper.addCaseDocumentWithDocumentType(legalAdviserId, "OTHER-TravelCard");
            caseDocumentHelper.findDocument(legalAdviserId, 0, "OTHER-TravelCard", 1);
            caseDocumentHelper.findDocument(legalAdviserId, 1, "OTHER-TravelCard", 2);
        }
    }

    @Test
    public void addCaseDocumentWithDocumentFileAndVerifyDocumentAdded() {
        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(createCasePayloadBuilder.getId())) {
            caseDocumentHelper.addCaseDocument();
            caseDocumentHelper.verifyInActiveMQ();
            caseDocumentHelper.verifyInPublicTopic();
            caseDocumentHelper.assertDocumentAdded();
        }
    }

    @Test
    public void addOtherDocumentAndVerifyNotVisibleForTflUser() {
        UUID tflUserId = UUID.randomUUID();
        UsersGroupsStub.stubGroupForUser(tflUserId, UsersGroupsStub.SJP_PROSECUTORS_GROUP);
        UsersGroupsStub.stubForUserDetails(tflUserId, ProsecutingAuthority.TFL);

        UUID courtAdminUserId = UUID.randomUUID();
        UsersGroupsStub.stubGroupForUser(courtAdminUserId, UsersGroupsStub.COURT_ADMINISTRATORS_GROUP);
        UsersGroupsStub.stubForUserDetails(courtAdminUserId, PROSECUTING_AUTHORITY_ACCESS_ALL);

        try (CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(createCasePayloadBuilder.getId())) {
            caseDocumentHelper.addCaseDocumentWithDocumentType(courtAdminUserId, "OTHER");
            caseDocumentHelper.verifyInActiveMQ();
            caseDocumentHelper.assertDocumentAdded(courtAdminUserId);
            caseDocumentHelper.verifyDocumentNotVisibleForProsecutorWhenQueryingForCaseDocuments(tflUserId);
            caseDocumentHelper.verifyDocumentNotVisibleForProsecutorWhenQueryingForACase(tflUserId);
        }
    }

    @Test
    public void shouldUploadPleaCaseDocument() {
        stubAddCaseMaterial();

        try (CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(createCasePayloadBuilder.getId())) {
            caseDocumentHelper.uploadPleaCaseDocument();
            final String documentReference = caseDocumentHelper.verifyCaseDocumentUploadedEventRaised();
            caseDocumentHelper.assertCaseMaterialAdded(documentReference);
        }
    }

    @Test
    public void addCaseDocumentRejectsWhenCaseIsInReferToCourtHearingStatus() {
        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(createCasePayloadBuilder.getId())) {


            final String sessionId = randomUUID().toString();
            final String referralReasonId = randomUUID().toString();
            final String hearingTypeId = randomUUID().toString();
            final UUID caseId = createCasePayloadBuilder.getId();
            final JsonObject eventPayload = createObjectBuilder()
                    .add("caseId", caseId.toString())
                    .add("sessionId", sessionId)
                    .add("referralReasonId", referralReasonId)
                    .add("hearingTypeId", hearingTypeId)
                    .add("estimatedHearingDuration", 30)
                    .add("decisionSavedAt", new UtcClock().now().toString())
                    .add("listingNotes", "wheelchair access required")
                    .build();

            final Metadata metadata = metadataFrom(JsonObjects.createObjectBuilder(
                    metadataBuilder()
                            .withName(PUBLIC_EVENT_SELECTOR_CASE_REFER_FOR_COURT_HEARING_IN_RESULTING)
                            .withUserId(randomUUID().toString())
                            .withId(UUID.randomUUID())
                            .createdAt(new UtcClock().now())
                            .build()
                            .asJsonObject()).build())
                    .build();

            sendPublicEvent(eventPayload, metadata, PUBLIC_EVENT_SELECTOR_CASE_REFER_FOR_COURT_HEARING_IN_RESULTING);
            caseDocumentHelper.verifyInPublicTopicCaseRefered();
            caseDocumentHelper.uploadPleaCaseDocument();
            caseDocumentHelper.verifyInActiveMQCaseUploadRejected();
        }
    }


    @Test
    public void addsDocumentNumberToDuplicateDocumentTypes() {
        final UUID userId = UUID.randomUUID();
        UsersGroupsStub.stubForUserDetails(userId, PROSECUTING_AUTHORITY_ACCESS_ALL);

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(createCasePayloadBuilder.getId())) {
            caseDocumentHelper.addCaseDocument();
            caseDocumentHelper.verifyInActiveMQ();
            caseDocumentHelper.verifyInPublicTopic();
            caseDocumentHelper.assertDocumentAdded(userId);
            caseDocumentHelper.findDocument(userId, 0, "SJPN", 1);
        }
    }
}
