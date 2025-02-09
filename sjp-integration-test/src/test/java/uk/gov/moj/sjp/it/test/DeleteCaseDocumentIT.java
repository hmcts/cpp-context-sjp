package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.PARTIAL_AOCP_CRITERIA_NOTIFICATION;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseNotAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubAddMapping;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubGetFromIdMapper;
import static uk.gov.moj.sjp.it.stub.MaterialStub.stubDeleteMaterial;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.DeleteCaseDocumentHelper;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.hamcrest.Matcher;
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
    private final DeleteCaseDocumentHelper deleteCaseDocumentHelper = new DeleteCaseDocumentHelper();

    @BeforeAll
    public static void setupOnce() {
        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(USER, "ALL");
        stubGroupForUser(USER.getUserId(), "Legal Advisers");
        stubAddMapping();
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

        cleanViewStore();

        stubForUserDetails(legalAdviser);
        stubForUserDetails(secondLineSupport);

        stubGroupForUser(legalAdviser.getUserId(), "Legal Advisers");
        stubGroupForUser(secondLineSupport.getUserId(), "Second Line Support");
        stubGetFromIdMapper(PARTIAL_AOCP_CRITERIA_NOTIFICATION.name(), caseId.toString(),
                "CASE_ID", caseId.toString());

        createCasePayloadBuilder = withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(POSTING_DATE);
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);
        pollUntilCaseNotAssignedToUser(caseId, systemUserId);

        try (CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.addCaseDocument(legalAdviserId, documentId, materialId, "OTHER-TravelCard");
            pollForCase(caseId, new Matcher[]{withJsonPath("$.caseDocuments[0].id", is(documentId.toString()))});
        }
    }

    @Test
    public void shouldAcceptDeleteCaseDocumentRequestWhenCaseNotInSessionAndFurtherCallDeleteMaterialDelete() {
        stubDeleteMaterial(materialId.toString());

        deleteCaseDocumentHelper.deleteCaseDocument(caseId, documentId, secondLineSupport.getUserId(), ACCEPTED);
        deleteCaseDocumentHelper.pollUntilPublicDeleteCaseDocumentRequestAcceptedEvent(caseId, documentId);
    }

    @Test
    public void shouldRejectDeleteCaseDocumentRequestWhenCaseInSession() {
        startSessionAndConfirm(sessionId, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignmentAndConfirm(sessionId, USER.getUserId(), caseId);

        deleteCaseDocumentHelper.deleteCaseDocument(caseId, documentId, secondLineSupport.getUserId(), ACCEPTED);
        deleteCaseDocumentHelper.pollUntilPublicDeleteCaseDocumentRequestRejectedEvent(caseId, documentId);
    }

}
