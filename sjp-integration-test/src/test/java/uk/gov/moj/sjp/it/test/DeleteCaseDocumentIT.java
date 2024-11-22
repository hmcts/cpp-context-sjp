package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.PARTIAL_AOCP_CRITERIA_NOTIFICATION;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseNotAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubAddMapping;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubGetFromIdMapper;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.stubNotifications;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubFixedLists;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForAllProsecutors;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypes;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.DeleteCaseDocumentHelper;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.MaterialStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteCaseDocumentIT extends BaseIntegrationTest {

    private UUID caseId, documentId, offenceId, materialId, sessionId, legalAdviserId;
    private User legalAdviser, secondLineSupport;
    private final UUID systemUserId = randomUUID();
    private static final String NATIONAL_COURT_CODE = "1080";
    private static final String DEFENDANT_REGION = "croydon";
    private final static LocalDate POSTING_DATE = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final static User USER = new User("John", "Rambo", randomUUID());

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private final DeleteCaseDocumentHelper deleteCaseDocumentHelper = new DeleteCaseDocumentHelper();

    @BeforeAll
    public static void setupOnce() {
        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        SchedulingStub.stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(USER, "ALL");
        stubGroupForUser(USER.getUserId(), "Legal Advisers");
        stubResultDefinitions();
        stubFixedLists();
        stubAllResultDefinitions();
        stubQueryForVerdictTypes();
        stubQueryForAllProsecutors();
        stubNotifications();
        stubAddMapping();
        stubResultIds();
    }

    @BeforeEach
    public void setUp() throws SQLException {
        caseId = randomUUID();
        documentId = randomUUID();
        materialId = randomUUID();
        offenceId = randomUUID();
        sessionId = randomUUID();
        legalAdviserId = randomUUID();

        legalAdviser = user()
                .withUserId(randomUUID())
                .withFirstName("John")
                .withLastName("Smith")
                .build();

        secondLineSupport = user()
                .withUserId(randomUUID())
                .withFirstName("Trevor")
                .withLastName("Wall")
                .build();

        databaseCleaner.cleanViewStore();

        stubForUserDetails(legalAdviser);
        stubForUserDetails(secondLineSupport);

        stubGroupForUser(legalAdviser.getUserId(), "Legal Advisers");
        stubGroupForUser(secondLineSupport.getUserId(), "Second Line Support");
        stubGetFromIdMapper(PARTIAL_AOCP_CRITERIA_NOTIFICATION.name(), caseId.toString(),
                "CASE_ID", caseId.toString());

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(POSTING_DATE);
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);


        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);
        pollUntilCaseNotAssignedToUser(caseId, systemUserId);

        try (CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.addCaseDocument(legalAdviserId, documentId, materialId, "OTHER-TravelCard");
            caseDocumentHelper.verifyInActiveMQ();
        }
    }

    @Test
    public void shouldReturn403WhenUnAuthorisedUserTryToPerformDeleteCaseDocumentRequest() {
        deleteCaseDocumentHelper.deleteCaseDocument(caseId, documentId, legalAdviser.getUserId(), FORBIDDEN);
    }

    @Test
    public void shouldAcceptDeleteCaseDocumentRequestWhenCaseNotInSessionAndFurtherCallDeleteMaterialDelete() {
        MaterialStub.stubDeleteMaterial(materialId.toString());

        deleteCaseDocumentHelper.deleteCaseDocument(caseId, documentId, secondLineSupport.getUserId(), ACCEPTED);
        deleteCaseDocumentHelper.pollUntilDeleteCaseDocumentRequestAcceptedEvent(caseId, documentId, materialId);
        deleteCaseDocumentHelper.pollUntilPublicDeleteCaseDocumentRequestAcceptedEvent(caseId, documentId);
    }

    @Test
    public void shouldRejectDeleteCaseDocumentRequestWhenCaseInSession() {
        startSession(sessionId, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, USER.getUserId());
        pollUntilCaseAssignedToUser(caseId, USER.getUserId());

        deleteCaseDocumentHelper.deleteCaseDocument(caseId, documentId, secondLineSupport.getUserId(), ACCEPTED);
        deleteCaseDocumentHelper.pollUntilDeleteCaseDocumentRequestRejectedEvent(caseId, documentId);
        deleteCaseDocumentHelper.pollUntilPublicDeleteCaseDocumentRequestRejectedEvent(caseId, documentId);
    }

}
