package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED_APPLICATION_PENDING;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier.ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION;
import static uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status.FAILED;
import static uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status.GENERATED;
import static uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status.GENERATION_FAILED;
import static uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status.QUEUED;
import static uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status.REQUIRED;
import static uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status.SENT;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.createCaseApplication;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseHasStatus;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubAddMapping;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubForIdMapperSuccess;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubGetFromIdMapper;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.publishNotificationFailedPublicEvent;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.verifyNotification;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByLjaCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByLocalLJACode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.stubGenerateDocumentEndPoint;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getPostCallResponse;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;
import static uk.gov.moj.sjp.it.util.SysDocGeneratorHelper.publishDocumentAvailablePublicEvent;
import static uk.gov.moj.sjp.it.util.SysDocGeneratorHelper.publishGenerationFailedPublicEvent;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.NotificationNotifyStub;
import uk.gov.moj.sjp.it.util.SjpViewstore;
import uk.gov.moj.sjp.it.util.builders.DischargeBuilder;
import uk.gov.moj.sjp.it.util.builders.FinancialImpositionBuilder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.google.common.collect.Sets;
import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnforcementPendingApplicationNotificationIT extends BaseIntegrationTest {

    private static final String ADD_FINANCIAL_IMPOSITION_WRITE_MEDIA_TYPE = "application/vnd.sjp.add-financial-imposition-account-number-bdf+json";
    private static final LocalDate POSTING_DATE = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private static final User USER = new User("John", "Smith", randomUUID());
    private static final LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);
    private static final String NATIONAL_COURT_CODE = "1080";
    private static final String NATIONAL_COURT_NAME = "Bedfordshire Magistrates' Court";
    private static final String DEFENDANT_REGION = "croydon";
    private static final String CREATE_CASE_APPLICATION_FILE = "CaseApplicationIT/sjp.command.create-case-application.json";
    private static final String CREATE_CASE_APPLICATION_FILE_FOR_ORGANISATION = "CaseApplicationIT/sjp.command.create-case-application-for-organisation.json";
    private static final UUID STAT_DEC_TYPE_ID = fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e");
    private static final String STAT_DEC_TYPE_CODE = "MC80528";
    private static final String APP_STATUS = "DRAFT";
    private final SjpViewstore sjpViewstore = new SjpViewstore();
    private UUID caseId;
    private UUID sessionId;
    private UUID offenceId;
    private UUID applicationId;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private static final String STAT_DECS_EMAIL_SUBJECT = "Subject: APPLICATION FOR A STATUTORY DECLARATION RECEIVED (COMMISSIONER OF OATHS)";

    @BeforeEach
    public void setUp() {
        stubForIdMapperSuccess(Response.Status.OK);
    }

    @Test
    void shouldRequestPdfEmailAttachmentGenerationViaSystemDocGeneratorAndSendToNotificationNotify() throws SQLException {
        createCase();
        completeCaseWithEndorsementsApplied(COMPLETED);
        createCaseApplicationStatDecs();

        stubGetFromIdMapper(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.name(), applicationId.toString(),
                "CASE_ID", "7242d476-9ca3-454a-93ee-78bf148602bf");
        stubAddMapping();

        final UUID documentFileServiceId = randomUUID();
        final UUID sourceCorrelationId = applicationId;
        sendSysDocGeneratorDocumentAvailablePublicEventAndWaitFor(
                sourceCorrelationId, documentFileServiceId
        );


        final JsonObject notification = verifyNotification(sourceCorrelationId, "LCCCCollectionUnit@hmcts.gsi.gov.uk");
        assertEmailSubject(notification);
        assertStatusUpdatedInViewstore(sourceCorrelationId, documentFileServiceId, QUEUED);
    }

    @Test
    void shouldUpdateViewstoreWhenSystemDocGeneratorGeneratedPublicEventIsReceived() throws SQLException {
        createCase();
        completeCaseWithEndorsementsApplied(COMPLETED);
        createCaseApplicationStatDecs();
        completeCaseWithEndorsementsApplied(COMPLETED_APPLICATION_PENDING);
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();
        sjpViewstore.insertNotificationOfEnforcementPendingStatus(sourceCorrelationId, null, REQUIRED, ZonedDateTime.now());

        sendSysDocGeneratorDocumentAvailablePublicEventAndWaitFor(
                sourceCorrelationId,
                documentFileServiceId
        );

        assertStatusUpdatedInViewstore(sourceCorrelationId, GENERATED);
    }

    @Test
    public void shouldUpdateViewstoreWhenSystemDocGeneratorGenerationFailedPublicEventIsReceived() throws SQLException {
        createCase();
        completeCaseWithEndorsementsApplied(COMPLETED);
        createCaseApplicationStatDecs();
        final UUID sourceCorrelationId = randomUUID();
        sjpViewstore.insertNotificationOfEnforcementPendingStatus(sourceCorrelationId, null, REQUIRED, ZonedDateTime.now());
        sendSysDocGeneratorGenerationFailedPublicEvent(sourceCorrelationId);

        assertStatusUpdatedInViewstore(sourceCorrelationId, GENERATION_FAILED);
    }

    @Test
    public void shouldUpdateViewstoreWhenNotificationNotifyNotificationFailedPublicEventIsReceived() {
        final UUID notificationId = givenNotificationOfEndorsementStatusIsPresentInViewstore();
        stubGetFromIdMapper(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.name(), notificationId.toString(),
                "CASE_ID", "7242d476-9ca3-454a-93ee-78bf148602bf");
        stubAddMapping();
        sendNotificationNotifyNotificationFailedPublicEvent(notificationId);

        assertStatusUpdatedInViewstore(notificationId, FAILED);
    }

    @Test
    public void shouldUpdateViewstoreWhenNotificationNotifyNotificationSentPublicEventIsReceived() {
        final UUID notificationId = givenNotificationOfEndorsementStatusIsPresentInViewstore();
        stubGetFromIdMapper(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.name(), notificationId.toString(),
                "CASE_ID", "7242d476-9ca3-454a-93ee-78bf148602bf");
        stubAddMapping();
        sendNotificationNotifyNotificationSentPublicEvent(notificationId);

        assertStatusUpdatedInViewstore(notificationId, SENT);
    }

    @Test
    public void shouldRequestPdfEmailAttachmentGenerationViaSystemDocGeneratorAndSendToNotificationNotifyForOrganisation() throws SQLException {
        createCase();
        completeCaseWithEndorsementsApplied(COMPLETED);
        createCaseApplicationStatDecsForOrganisation();

        stubGetFromIdMapper(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.name(), applicationId.toString(),
                "CASE_ID", "7242d476-9ca3-454a-93ee-78bf148602bf");
        stubAddMapping();

        final UUID documentFileServiceId = randomUUID();
        final UUID sourceCorrelationId = applicationId;
        sendSysDocGeneratorDocumentAvailablePublicEventAndWaitFor(
                sourceCorrelationId, documentFileServiceId
        );

        final JsonObject notification = verifyNotification(sourceCorrelationId, "LCCCCollectionUnit@hmcts.gsi.gov.uk");
        assertEmailSubject(notification);
        assertStatusUpdatedInViewstore(sourceCorrelationId, documentFileServiceId, QUEUED);
    }

    private UUID givenNotificationOfEndorsementStatusIsPresentInViewstore() {
        final UUID applicationDecisionId = randomUUID();
        sjpViewstore.insertNotificationOfEnforcementPendingStatus(applicationDecisionId, null, QUEUED, ZonedDateTime.now());
        return applicationDecisionId;
    }

    private void createCaseApplicationStatDecs() {
        createCaseApplication(USER.getUserId(), caseId, applicationId,
                STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                CREATE_CASE_APPLICATION_FILE);

        pollUntilCaseReady(caseId);
    }

    private void createCaseApplicationStatDecsForOrganisation() {
        createCaseApplication(USER.getUserId(), caseId, applicationId,
                STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                CREATE_CASE_APPLICATION_FILE_FOR_ORGANISATION);

        pollUntilCaseReady(caseId);
    }

    private void completeCaseWithEndorsementsApplied(uk.gov.moj.cpp.sjp.domain.common.CaseStatus status) {
        startSessionAndConfirm(sessionId, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignmentAndConfirm(sessionId, USER.getUserId(), caseId);

        final Discharge discharge = DischargeBuilder.withDefaults()
                .id(offenceId)
                .withDischargeType(CONDITIONAL)
                .withDischargedFor(new DischargePeriod(2, MONTH))
                .withVerdict(FOUND_GUILTY)
                .withCompensation(new BigDecimal(230))
                .withDisqualification(DisqualificationType.POINTS)
                .build();
        discharge.getOffenceDecisionInformation().setPressRestrictable(false);

        final DecisionCommand decision = new DecisionCommand(
                sessionId,
                caseId,
                null,
                USER,
                asList(discharge),
                FinancialImpositionBuilder.withDefaults());

        saveDecision(decision);
        pollUntilCaseHasStatus(caseId, status);

        addFinancialImposition(caseId, createCasePayloadBuilder.getDefendantBuilder().getId(), Response.Status.ACCEPTED);
    }

    private void createCase() throws SQLException {
        caseId = randomUUID();
        offenceId = randomUUID();
        sessionId = randomUUID();
        applicationId = randomUUID();

        cleanViewStore();

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(USER, "ALL");
        stubGroupForUser(USER.getUserId(), "Legal Advisers");
        stubGenerateDocumentEndPoint();

        createCasePayloadBuilder = withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(POSTING_DATE);
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(
                createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(),
                NATIONAL_COURT_CODE,
                NATIONAL_COURT_NAME);
        stubEnforcementAreaByLocalLJACode(
                NATIONAL_COURT_CODE,
                NATIONAL_COURT_NAME);
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);
        stubEnforcementAreaByLjaCode();
        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);
    }

    private void sendSysDocGeneratorGenerationFailedPublicEvent(final UUID sourceCorrelationId) {
        publishGenerationFailedPublicEvent(
                sourceCorrelationId,
                ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.getValue()
        );
    }

    private void sendSysDocGeneratorDocumentAvailablePublicEventAndWaitFor(final UUID sourceCorrelationId, final UUID documentFileServiceId) {
        publishDocumentAvailablePublicEvent(sourceCorrelationId, ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.getValue(), documentFileServiceId);
    }

    private void sendNotificationNotifyNotificationFailedPublicEvent(final UUID notificationId) {
        publishNotificationFailedPublicEvent(notificationId);
    }

    private void sendNotificationNotifyNotificationSentPublicEvent(final UUID notificationId) {
        NotificationNotifyStub.publishNotificationSentPublicEvent(notificationId);
    }

    private void assertStatusUpdatedInViewstore(final UUID applicationId,
                                                final UUID fileId,
                                                final EnforcementNotification.Status status) {
        pollUntil(() -> sjpViewstore.countNotificationOfEnforcementPendingApplicationEmails(applicationId, status), greaterThan(0));
    }

    private void assertStatusUpdatedInViewstore(final UUID applicationId,
                                                final EnforcementNotification.Status status) {
        pollUntil(() -> sjpViewstore.countNotificationOfEnforcementPendingApplicationEmails(applicationId, status), greaterThan(0));
    }

    private void pollUntil(final Callable<Integer> function, final Matcher<Integer> matcher) {
        Awaitility.await()
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(function, matcher);
    }

    private void assertEmailSubject(final JsonObject notification) {
        assertThat(notification.getJsonObject("personalisation").getString("subject"), equalTo(STAT_DECS_EMAIL_SUBJECT));
    }

    public String addFinancialImposition(final UUID caseId, final UUID defendantId, final Response.Status status) {
        final JsonObjectBuilder payload = JsonObjects.createObjectBuilder();
        payload.add("correlationId", caseId.toString())
                .add("accountNumber", "12345678");

        return getPostCallResponse("/cases/" + caseId + "/defendant/" + defendantId.toString() + "/add-financial-imposition-account-number-bdf", ADD_FINANCIAL_IMPOSITION_WRITE_MEDIA_TYPE, payload.build().toString(), status);
    }
}
