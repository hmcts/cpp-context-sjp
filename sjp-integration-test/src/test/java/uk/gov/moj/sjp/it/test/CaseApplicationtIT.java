
package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.APPLICATION_PENDING;
import static uk.gov.moj.cpp.sjp.domain.Priority.HIGH;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.PARTIAL_AOCP_CRITERIA_NOTIFICATION;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.defaultCaseBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.assignCaseToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseNotAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.createCaseApplication;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSaved;
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
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorSent;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.ReadyCaseHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.NotificationNotifyStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

public class CaseApplicationtIT extends BaseIntegrationTest {

    private UUID caseId;
    private UUID sessionId;
    private UUID offenceId;
    private UUID appId;
    private final UUID systemUserId = randomUUID();
    private final String SJP_EVENT_CASE_APP_RECORDED = "sjp.events.case-application-recorded";
    private final String SJP_EVENT_CASE_APP_STAT_DEC = "sjp.events.case-stat-dec-recorded";
    private final String SJP_EVENT_CASE_APP_REOPENING = "sjp.events.case-application-for-reopening-recorded";
    private final String SJP_EVENT_CASE_APP_REJECTED = "sjp.events.case-application-rejected";
    private final String MARK_CASE_READY_FOR_DECISION_EVENT_NAME = "sjp.events.case-marked-ready-for-decision";
    public static final String CASE_STATUS_CHANGED = "sjp.events.case-status-changed";
    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private final EventListener eventListener = new EventListener();
    private final static LocalDate POSTING_DATE = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final static User USER = new User("John", "Smith", randomUUID());
    private final static LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);
    private static final String NATIONAL_COURT_CODE = "1080";
    private static final String DEFENDANT_REGION = "croydon";
    private static final UUID STAT_DEC_TYPE_ID = fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e");
    private static final String STAT_DEC_TYPE_CODE = "MC80528";
    private static final UUID REOPENING_TYPE_ID = fromString("44c238d9-3bc2-3cf3-a2eb-a7d1437b8383");
    private static final String REOPENING_TYPE_CODE = "MC80524";
    private static final String APP_STATUS = "DRAFT";
    private static final String UNRECOGNIZED_APP = "Unrecognized application type or code";
    private static final String createCaseApplicationFile = "CaseApplicationIT/sjp.command.create-case-application.json";

    @Before
    public void setUp() throws SQLException {
        caseId = randomUUID();
        offenceId = randomUUID();
        sessionId = randomUUID();
        appId = randomUUID();

        databaseCleaner.cleanViewStore();

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
        stubGetFromIdMapper(PARTIAL_AOCP_CRITERIA_NOTIFICATION.name(), caseId.toString(),
                "CASE_ID", caseId.toString());
        stubAddMapping();
        stubResultIds();

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

        startSession(sessionId, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, USER.getUserId());

        final Discharge discharge = createDischarge(null, createOffenceDecisionInformation(offenceId, FOUND_GUILTY), CONDITIONAL, new DischargePeriod(2, MONTH), new BigDecimal(230), null, false, null, null);
        discharge.getOffenceDecisionInformation().setPressRestrictable(false);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, USER, asList(discharge), financialImposition());


        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        verifyDecisionSaved(decision, decisionSaved);
    }

    @Test
    public void shouldRecordCaseStartDec() {
        final ReadyCaseHelper readyCaseHelper = new ReadyCaseHelper();
        eventListener.subscribe(SJP_EVENT_CASE_APP_RECORDED, SJP_EVENT_CASE_APP_STAT_DEC)
                .run(() -> createCaseApplication(USER.getUserId(), caseId, appId,
                        STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                        createCaseApplicationFile));

        final Optional<JsonEnvelope> caseApplicationRecordedEnv = eventListener.popEvent(SJP_EVENT_CASE_APP_RECORDED);
        final Optional<JsonEnvelope> caseStatDecRecorded = eventListener.popEvent(SJP_EVENT_CASE_APP_STAT_DEC);
        assertThat(caseApplicationRecordedEnv.isPresent(), is(true));

        assertThat(caseApplicationRecordedEnv.get(), jsonEnvelope(
                metadata().withName(SJP_EVENT_CASE_APP_RECORDED),
                payloadIsJson((allOf(
                        withJsonPath("$.courtApplication.id", equalTo(appId.toString())),
                        withJsonPath("$.courtApplication.applicationReceivedDate", equalTo(DATE_RECEIVED.toString())),
                        withJsonPath("$.courtApplication.applicationStatus", equalTo(APP_STATUS)),
                        withJsonPath("$.courtApplication.applicationReference", notNullValue()),
                        withJsonPath("$.courtApplication.type.code", equalTo(STAT_DEC_TYPE_CODE))
                )))));
        assertThat(caseStatDecRecorded.get(), jsonEnvelope(
                metadata().withName(SJP_EVENT_CASE_APP_STAT_DEC),
                payloadIsJson((allOf(
                        withJsonPath("$.applicant", notNullValue()),
                        withJsonPath("$.applicationId", notNullValue())
                )))));

        pollUntilCaseReady(caseId);

        readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, APPLICATION_PENDING, MAGISTRATE, HIGH);

    }

    @Test
    public void shouldRecordCaseFOR_Reopening_application() {

        eventListener.subscribe(
                SJP_EVENT_CASE_APP_RECORDED,
                SJP_EVENT_CASE_APP_REOPENING)
                .run(() -> createCaseApplication(USER.getUserId(), caseId, appId,
                        REOPENING_TYPE_ID, REOPENING_TYPE_CODE, "A",  DATE_RECEIVED, APP_STATUS, createCaseApplicationFile));
        final Optional<JsonEnvelope> caseApplicationForReopeningRecorded = eventListener.popEvent(SJP_EVENT_CASE_APP_REOPENING);
        assertThat(caseApplicationForReopeningRecorded.isPresent(), is(true));
        assertThat(caseApplicationForReopeningRecorded.get(), jsonEnvelope(
                metadata().withName(SJP_EVENT_CASE_APP_REOPENING),
                payloadIsJson((allOf(
                        withJsonPath("$.applicant", notNullValue()),
                        withJsonPath("$.applicationId", notNullValue())

                )))));
    }

    @Test
    public void shouldRejectApplication() {

        eventListener.subscribe(
                SJP_EVENT_CASE_APP_REJECTED).run(() -> createCaseApplication(USER.getUserId(), caseId, appId,
                REOPENING_TYPE_ID, "", "A",  DATE_RECEIVED, APP_STATUS, createCaseApplicationFile));
        final Optional<JsonEnvelope> caseApplicationRejected = eventListener.popEvent(SJP_EVENT_CASE_APP_REJECTED);
        assertThat(caseApplicationRejected.isPresent(), is(true));
        assertThat(caseApplicationRejected.get(), jsonEnvelope(
                metadata().withName(SJP_EVENT_CASE_APP_REJECTED),
                payloadIsJson((allOf(
                        withJsonPath("$.applicationId", equalTo(appId.toString())),
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.description", equalTo(UNRECOGNIZED_APP))
                )))));
    }

    @Test
    public void shouldRecordMarkCaseReadyForDecision() {

        eventListener.subscribe(MARK_CASE_READY_FOR_DECISION_EVENT_NAME)
                .run(() -> createCaseApplication(USER.getUserId(), caseId, appId,
                        STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                        createCaseApplicationFile));

        final Optional<JsonEnvelope> event = eventListener.popEvent(MARK_CASE_READY_FOR_DECISION_EVENT_NAME);
        assertThat(event.isPresent(), is(true));

        eventListener.subscribe(PartialAocpCriteriaNotificationProsecutorSent.EVENT_NAME)
                .run(() -> NotificationNotifyStub.publishNotificationSentPublicEvent(caseId));

        final Optional<JsonEnvelope> emailEvent = eventListener.popEvent(PartialAocpCriteriaNotificationProsecutorSent.EVENT_NAME);

        assertThat(emailEvent.isPresent(), is(true));
    }

    @Test
    public void shouldRecordCaseChangeStatus() {

        eventListener.subscribe(CASE_STATUS_CHANGED)
                .run(() -> createCaseApplication(USER.getUserId(), caseId, appId,
                        STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                        createCaseApplicationFile));

        final Optional<JsonEnvelope> event = eventListener.popEvent(CASE_STATUS_CHANGED);
        assertThat(event.isPresent(), is(true));

    }

    @Test
    public void  shouldAssignCaseWithApplicationPendingToUser() {
        final ReadyCaseHelper readyCaseHelper = new ReadyCaseHelper();

        createCaseAndWaitUntilReady(defaultCaseBuilder().withId(caseId));

        eventListener.subscribe(MARK_CASE_READY_FOR_DECISION_EVENT_NAME)
                .run(() -> createCaseApplication(USER.getUserId(), caseId, appId,
                        STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                        createCaseApplicationFile));
        pollUntilCaseReady(caseId);
        readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, APPLICATION_PENDING, MAGISTRATE, HIGH);
        pollUntilCaseNotAssignedToUser(caseId, systemUserId);
        assignCaseToUser(caseId, USER.getUserId(), systemUserId, ACCEPTED);
        pollUntilCaseAssignedToUser(caseId, USER.getUserId());

    }

    private FinancialImposition financialImposition() {
        return new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(120), null, new BigDecimal(32), null, null, true),
                new Payment(new BigDecimal(272), PAY_TO_COURT, "No information from defendant", null,
                        new PaymentTerms(false, null, new Installments(new BigDecimal(20), InstallmentPeriod.MONTHLY, LocalDate.now().plusDays(30))), (new CourtDetails(NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court"))
                ));
    }

    private static void createCaseAndWaitUntilReady(final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder) {
        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder))
                .run(() -> (createCasePayloadBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);
    }

}

