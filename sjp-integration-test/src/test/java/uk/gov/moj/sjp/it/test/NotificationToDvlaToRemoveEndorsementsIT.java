package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.ENDORSEMENT_REMOVAL_NOTIFICATION;
import static uk.gov.moj.cpp.sjp.event.processor.service.notification.EndorsementRemovalNotificationTemplateDataBuilder.formatDate;
import static uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier.NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT;
import static uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus.Status.FAILED;
import static uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus.Status.GENERATED;
import static uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus.Status.GENERATION_FAILED;
import static uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus.Status.QUEUED;
import static uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus.Status.SENT;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.createCaseApplication;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.saveApplicationDecision;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseStatusCompleted;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubAddMapping;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubGetFromIdMapper;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.publishNotificationFailedPublicEvent;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.publishNotificationSentPublicEvent;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.verifyNotification;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDvlaPenaltyPointNotificationEmailAddress;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByLjaCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.pollSysDocGenerationRequests;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.stubGenerateDocumentEndPoint;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;
import static uk.gov.moj.sjp.it.util.SysDocGeneratorHelper.publishDocumentAvailablePublicEvent;
import static uk.gov.moj.sjp.it.util.SysDocGeneratorHelper.publishGenerationFailedPublicEvent;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.event.processor.service.notification.EndorsementRemovalNotificationEmailSubject;
import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.SjpViewstore;
import uk.gov.moj.sjp.it.util.builders.DischargeBuilder;
import uk.gov.moj.sjp.it.util.builders.FinancialImpositionBuilder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import com.google.common.collect.Sets;
import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class NotificationToDvlaToRemoveEndorsementsIT extends BaseIntegrationTest {

    private static final LocalDate POSTING_DATE = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private static final User USER = new User("John", "Smith", randomUUID());
    private static final LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);
    private static final String NATIONAL_COURT_CODE = "1080";
    private static final String NATIONAL_COURT_NAME = "Bedfordshire Magistrates' Court";
    private static final String DEFENDANT_REGION = "croydon";
    private static final String CREATE_CASE_APPLICATION_FILE = "CaseApplicationIT/sjp.command.create-case-application.json";
    private static final UUID STAT_DEC_TYPE_ID = fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e");
    private static final String STAT_DEC_TYPE_CODE = "MC80528";
    private static final String APP_STATUS = "DRAFT";
    private static final String SJP_EVENT_APPLICATION_DECISION_SAVED = "sjp.events.application-decision-saved";
    private final EventListener eventListener = new EventListener();
    private final SjpViewstore sjpViewstore = new SjpViewstore();
    private UUID caseId;
    private UUID sessionId;
    private UUID offenceId;
    private UUID applicationId;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Test
    public void shouldRequestPdfEmailAttachmentGenerationViaSystemDocGeneratorAndSendToNotificationNotify() throws SQLException {
        createCase();
        completeCaseWithEndorsementsApplied();
        createCaseApplicationStatDecs();
        final String dvlaEmailAddress = stubDvlaPenaltyPointNotificationEmailAddress();
        final JsonObject applicationDecisionSaved = saveApplicationGrantedDecision();
        final String applicationDecisionId = applicationDecisionSaved.getString("decisionId");
        stubGetFromIdMapper(ENDORSEMENT_REMOVAL_NOTIFICATION.name(), applicationDecisionId,
                "CASE_ID", "7242d476-9ca3-454a-93ee-78bf148602bf");
        stubAddMapping();

        final JSONObject generationRequests = pollSysDocGenerationRequests(hasSize(1)).get(0);
        assertSysDocRequest(applicationDecisionId, generationRequests);

        final UUID documentFileServiceId = randomUUID();
        final UUID sourceCorrelationId = fromString(applicationDecisionId);
        sendSysDocGeneratorDocumentAvailablePublicEvent(
                sourceCorrelationId, documentFileServiceId
        );

        final JsonObject notification = verifyNotification(sourceCorrelationId, dvlaEmailAddress);
        assertEmailSubject(notification);

        assertStatusUpdatedInViewstore(sourceCorrelationId, documentFileServiceId, QUEUED);
    }

    @Test
    public void shouldUpdateViewstoreWhenSystemDocGeneratorGeneratedPublicEventIsReceived() {
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        sendSysDocGeneratorDocumentAvailablePublicEvent(
                sourceCorrelationId,
                documentFileServiceId
        );

        assertStatusUpdatedInViewstore(sourceCorrelationId, GENERATED);
    }

    @Test
    public void shouldUpdateViewstoreWhenSystemDocGeneratorGenerationFailedPublicEventIsReceived() {
        final UUID sourceCorrelationId = randomUUID();

        sendSysDocGeneratorGenerationFailedPublicEvent(sourceCorrelationId);

        assertStatusUpdatedInViewstore(sourceCorrelationId, GENERATION_FAILED);
    }

    @Test
    public void shouldUpdateViewstoreWhenNotificationNotifyNotificationFailedPublicEventIsReceived() {
        final UUID notificationId = givenNotificationOfEndorsementStatusIsPresentInViewstore();
        stubGetFromIdMapper(ENDORSEMENT_REMOVAL_NOTIFICATION.name(), notificationId.toString(),
                "CASE_ID", "7242d476-9ca3-454a-93ee-78bf148602bf");
        sendNotificationNotifyNotificationFailedPublicEvent(notificationId);

        assertStatusUpdatedInViewstore(notificationId, FAILED);
    }

    @Test
    public void shouldUpdateViewstoreWhenNotificationNotifyNotificationSentPublicEventIsReceived() {
        final UUID notificationId = givenNotificationOfEndorsementStatusIsPresentInViewstore();
        stubGetFromIdMapper(ENDORSEMENT_REMOVAL_NOTIFICATION.name(), notificationId.toString(),
                "CASE_ID", "7242d476-9ca3-454a-93ee-78bf148602bf");
        sendNotificationNotifyNotificationSentPublicEvent(notificationId);

        assertStatusUpdatedInViewstore(notificationId, SENT);
    }

    private UUID givenNotificationOfEndorsementStatusIsPresentInViewstore() {
        final UUID applicationDecisionId = randomUUID();
        sjpViewstore.insertNotificationOfEndorsementStatus(applicationDecisionId, null, QUEUED, ZonedDateTime.now());
        return applicationDecisionId;
    }

    private JsonObject saveApplicationGrantedDecision() {
        sessionId = startNewSession();
        eventListener.subscribe(SJP_EVENT_APPLICATION_DECISION_SAVED)
                .run(() -> saveApplicationDecision(USER.getUserId(), caseId, applicationId, sessionId, true, false, null, null));
        final Optional<JsonEnvelope> applicationDecisionSaved = eventListener.popEvent(SJP_EVENT_APPLICATION_DECISION_SAVED);
        assertThat(applicationDecisionSaved.isPresent(), is(true));
        return applicationDecisionSaved.get().payloadAsJsonObject();
    }

    private UUID startNewSession() {
        final UUID sessionId2 = randomUUID();
        startSessionAndConfirm(sessionId2, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignmentAndConfirm(sessionId2, USER.getUserId(), caseId);
        return sessionId2;
    }

    private void createCaseApplicationStatDecs() {
        createCaseApplication(USER.getUserId(), caseId, applicationId,
                STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                CREATE_CASE_APPLICATION_FILE);

        pollUntilCaseReady(caseId);
    }

    private void completeCaseWithEndorsementsApplied() {
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
        pollUntilCaseStatusCompleted(caseId);
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

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(POSTING_DATE);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(
                createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(),
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
                NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue());
    }

    private void sendSysDocGeneratorDocumentAvailablePublicEvent(final UUID sourceCorrelationId, final UUID documentFileServiceId) {
        publishDocumentAvailablePublicEvent(
                sourceCorrelationId,
                NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue(),
                documentFileServiceId);
    }

    private void sendNotificationNotifyNotificationFailedPublicEvent(final UUID notificationId) {
        publishNotificationFailedPublicEvent(notificationId);
    }

    private void sendNotificationNotifyNotificationSentPublicEvent(final UUID notificationId) {
        publishNotificationSentPublicEvent(notificationId);
    }

    private void assertStatusUpdatedInViewstore(final UUID applicationDecisionId,
                                                final UUID fileId,
                                                final NotificationOfEndorsementStatus.Status status) {
        pollUntil(() -> sjpViewstore.countNotificationOfEndorsementStatus(applicationDecisionId, status, fileId), greaterThan(0));
    }

    private void assertStatusUpdatedInViewstore(final UUID applicationDecisionId,
                                                final NotificationOfEndorsementStatus.Status status) {
        pollUntil(() -> sjpViewstore.countNotificationOfEndorsementStatus(applicationDecisionId, status), greaterThan(0));
    }

    private void pollUntil(final Callable<Integer> function, final Matcher<Integer> matcher) {
        Awaitility.await()
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(function, matcher);
    }

    private void assertSysDocRequest(final String applicationDecisionId, final JSONObject generationRequests) {
        assertThat(generationRequests.getString("originatingSource"), equalTo("sjp"));
        assertThat(generationRequests.getString("sourceCorrelationId"), equalTo(applicationDecisionId));
        assertThat(generationRequests.getString("templateIdentifier"), equalTo(NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue()));
        assertThat(generationRequests.getString("conversionFormat"), is("pdf"));
        assertThat(generationRequests.has("payloadFileServiceId"), is(true));
    }

    private void assertEmailSubject(final JsonObject notification) {
        final EndorsementRemovalNotificationEmailSubject subject = new EndorsementRemovalNotificationEmailSubject(NATIONAL_COURT_NAME,
                createCasePayloadBuilder.getDefendantBuilder().getFirstName(),
                createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                formatDate(createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth()),
                createCasePayloadBuilder.getUrn());
        assertThat(notification.getJsonObject("personalisation").getString("subject"), equalTo(subject.toString()));
    }
}
