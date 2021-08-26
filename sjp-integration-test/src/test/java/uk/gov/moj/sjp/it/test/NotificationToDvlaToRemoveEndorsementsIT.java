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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.createCaseApplication;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.saveApplicationDecision;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubAddMapping;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubGetFromIdMapper;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.stubNotifications;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.verifyNotification;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDvlaPenaltyPointNotificationEmailAddress;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByLjaCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.pollSysDocGenerationRequests;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.stubGenerateDocumentEndPoint;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsFailed;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerated;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerationFailed;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsSent;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.service.notification.EndorsementRemovalNotificationEmailSubject;
import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.NotificationNotifyStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.SjpViewstore;
import uk.gov.moj.sjp.it.util.SysDocGeneratorHelper;
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
import com.jayway.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.Test;

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
    private static final String SJP_EVENT_CASE_APP_RECORDED = "sjp.events.case-application-recorded";
    private static final String SJP_EVENT_CASE_APP_STAT_DEC = "sjp.events.case-stat-dec-recorded";
    private static final String SJP_EVENT_APPLICATION_DECISION_SAVED = "sjp.events.application-decision-saved";
    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
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
        stubNotifications();
        final String dvlaEmailAddress = stubDvlaPenaltyPointNotificationEmailAddress();
        final JsonObject applicationDecisionSaved = saveApplicationGrantedDecision();
        final String applicationDecisionId = applicationDecisionSaved.getString("decisionId");
        stubGetFromIdMapper(ENDORSEMENT_REMOVAL_NOTIFICATION.name(), applicationDecisionId,
                "CASE_ID", "1ac91935-4f82-4a4f-bd17-fb50397e42dd");
        stubAddMapping();

        final JSONObject generationRequests = pollSysDocGenerationRequests(hasSize(1)).get(0);
        assertSysDocRequest(applicationDecisionId, generationRequests);

        final UUID documentFileServiceId = randomUUID();
        final UUID sourceCorrelationId = fromString(applicationDecisionId);
        final JsonObject actual = sendSysDocGeneratorDocumentAvailablePublicEventAndWaitFor(
                sourceCorrelationId, documentFileServiceId,
                NotificationToRemoveEndorsementsGenerated.EVENT_NAME
        );
        assertThat(actual.getString("applicationDecisionId"), equalTo(applicationDecisionId));

        final JsonObject notification = verifyNotification(sourceCorrelationId, dvlaEmailAddress);
        assertEmailSubject(notification);

        assertStatusUpdatedInViewstore(sourceCorrelationId, documentFileServiceId, QUEUED);
    }

    @Test
    public void shouldUpdateViewstoreWhenSystemDocGeneratorGeneratedPublicEventIsReceived() {
        stubNotifications();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final JsonObject actual = sendSysDocGeneratorDocumentAvailablePublicEventAndWaitFor(
                sourceCorrelationId,
                documentFileServiceId,
                NotificationToRemoveEndorsementsGenerated.EVENT_NAME
        );

        assertThat(actual.getString("applicationDecisionId"), equalTo(sourceCorrelationId.toString()));
        assertThat(actual.getString("fileId"), equalTo(documentFileServiceId.toString()));
        assertStatusUpdatedInViewstore(sourceCorrelationId, GENERATED);
    }

    @Test
    public void shouldUpdateViewstoreWhenSystemDocGeneratorGenerationFailedPublicEventIsReceived() {
        final UUID sourceCorrelationId = randomUUID();

        final JsonObject actual = sendSysDocGeneratorGenerationFailedPublicEvent(sourceCorrelationId);

        assertThat(actual.getString("applicationDecisionId"), equalTo(sourceCorrelationId.toString()));
        assertStatusUpdatedInViewstore(sourceCorrelationId, GENERATION_FAILED);
    }

    @Test
    public void shouldUpdateViewstoreWhenNotificationNotifyNotificationFailedPublicEventIsReceived() {
        final UUID notificationId = givenNotificationOfEndorsementStatusIsPresentInViewstore();
        stubGetFromIdMapper(ENDORSEMENT_REMOVAL_NOTIFICATION.name(), notificationId.toString(),
                "CASE_ID", "1ac91935-4f82-4a4f-bd17-fb50397e42dd");
        final JsonObject actual = sendNotificationNotifyNotificationFailedPublicEvent(notificationId);

        assertThat(actual.getString("applicationDecisionId"), equalTo(notificationId.toString()));
        assertThat(actual.getString("failedTime"), not(nullValue()));
        assertStatusUpdatedInViewstore(notificationId, FAILED);
    }

    @Test
    public void shouldUpdateViewstoreWhenNotificationNotifyNotificationSentPublicEventIsReceived() {
        final UUID notificationId = givenNotificationOfEndorsementStatusIsPresentInViewstore();
        stubGetFromIdMapper(ENDORSEMENT_REMOVAL_NOTIFICATION.name(), notificationId.toString(),
                "CASE_ID", "1ac91935-4f82-4a4f-bd17-fb50397e42dd");
        final JsonObject actual = sendNotificationNotifyNotificationSentPublicEvent(notificationId);

        assertThat(actual.getString("applicationDecisionId"), equalTo(notificationId.toString()));
        assertThat(actual.getString("sentTime"), not(nullValue()));
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
        startSession(sessionId2, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId2, USER.getUserId());
        return sessionId2;
    }

    private void createCaseApplicationStatDecs() {
        eventListener.subscribe(SJP_EVENT_CASE_APP_RECORDED, SJP_EVENT_CASE_APP_STAT_DEC)
                .run(() -> createCaseApplication(USER.getUserId(), caseId, applicationId,
                        STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                        CREATE_CASE_APPLICATION_FILE));

        pollUntilCaseReady(caseId);
    }

    private void completeCaseWithEndorsementsApplied() {
        startSession(sessionId, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, USER.getUserId());

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

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(() -> saveDecision(decision));
    }

    private void createCase() throws SQLException {
        caseId = randomUUID();
        offenceId = randomUUID();
        sessionId = randomUUID();
        applicationId = randomUUID();

        databaseCleaner.cleanViewStore();

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        AssignmentStub.stubAssignmentReplicationCommands();
        SchedulingStub.stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(USER, "ALL");
        stubGroupForUser(USER.getUserId(), "Legal Advisers");
        stubResultDefinitions();
        stubResultIds();
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
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);
    }

    private JsonObject sendSysDocGeneratorGenerationFailedPublicEvent(final UUID sourceCorrelationId) {
        eventListener.subscribe(NotificationToRemoveEndorsementsGenerationFailed.EVENT_NAME)
                .run(() -> SysDocGeneratorHelper.publishGenerationFailedPublicEvent(
                        sourceCorrelationId,
                        NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue()
                ));

        final Optional<JsonEnvelope> event = eventListener.popEvent(NotificationToRemoveEndorsementsGenerationFailed.EVENT_NAME);

        assertThat(event.isPresent(), is(true));
        return event.get().payloadAsJsonObject();
    }

    private JsonObject sendSysDocGeneratorDocumentAvailablePublicEventAndWaitFor(final UUID sourceCorrelationId, final UUID documentFileServiceId, final String eventName) {
        eventListener.subscribe(eventName).run(() ->
                SysDocGeneratorHelper.publishDocumentAvailablePublicEvent(
                        sourceCorrelationId,
                        NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue(),
                        documentFileServiceId)
        );

        final Optional<JsonEnvelope> event = eventListener.popEvent(eventName);

        assertThat(event.isPresent(), is(true));
        return event.get().payloadAsJsonObject();
    }

    private JsonObject sendNotificationNotifyNotificationFailedPublicEvent(final UUID notificationId) {
        eventListener.subscribe(NotificationToRemoveEndorsementsFailed.EVENT_NAME)
                .run(() -> NotificationNotifyStub.publishNotificationFailedPublicEvent(notificationId));

        final Optional<JsonEnvelope> event = eventListener.popEvent(NotificationToRemoveEndorsementsFailed.EVENT_NAME);

        assertThat(event.isPresent(), is(true));
        return event.get().payloadAsJsonObject();
    }

    private JsonObject sendNotificationNotifyNotificationSentPublicEvent(final UUID notificationId) {
        eventListener.subscribe(NotificationToRemoveEndorsementsSent.EVENT_NAME)
                .run(() -> NotificationNotifyStub.publishNotificationSentPublicEvent(notificationId));

        final Optional<JsonEnvelope> event = eventListener.popEvent(NotificationToRemoveEndorsementsSent.EVENT_NAME);

        assertThat(event.isPresent(), is(true));
        return event.get().payloadAsJsonObject();
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
                .timeout(3, TimeUnit.SECONDS)
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
