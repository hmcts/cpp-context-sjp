package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.createCaseApplication;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.saveApplicationDecision;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseNotReady;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseCompleted;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSaved;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.setPleas;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.setPleasAsUser;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.setPleasPayloadBuilder;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubFixedLists;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForAllProsecutors;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypes;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.builders.DismissBuilder.withDefaults;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.SetPleasHelper.SetPleasPayloadBuilder;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.PleaInfo;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class CaseApplicationDecisionIT extends BaseIntegrationTest {

    public static final String SJP_EVENTS_CASE_COMPLETED = "sjp.events.case-completed";
    private UUID caseId;
    private UUID sessionId;
    private UUID offenceId;
    private UUID appId;

    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private final EventListener eventListener = new EventListener();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    private final static LocalDate POSTING_DATE = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final static User USER = new User("John", "Smith", randomUUID());
    private final static LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);
    private static final String NATIONAL_COURT_CODE = "1080";
    private static final String DEFENDANT_REGION = "croydon";
    private static final String createCaseApplicationFile = "CaseApplicationIT/sjp.command.create-case-application.json";
    private final String SJP_EVENT_CASE_APP_RECORDED = "sjp.events.case-application-recorded";
    private final String SJP_EVENT_CASE_APP_STAT_DEC = "sjp.events.case-stat-dec-recorded";
    private final String SJP_EVENT_APPLICATION_DECISION_SAVED = "sjp.events.application-decision-saved";
    private static final String SJP_EVENT_APPLICATION_STATUS_CHANGED = ApplicationStatusChanged.EVENT_NAME;
    private static final String SJP_EVENT_APPLICATION_DECISION_SET_ASIDE = ApplicationDecisionSetAside.EVENT_NAME;
    private static final String SJP_EVENT_CASE_UNASSIGNED = CaseUnassigned.EVENT_NAME;
    private static final String SJP_EVENT_PLEAS_SET = PleasSet.EVENT_NAME;
    private static final String PUBLIC_SJP_APPLICATION_DECISION_SET_ASIDE = "public.sjp.application-decision-set-aside";
    private static final UUID STAT_DEC_TYPE_ID = fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e");
    private static final String STAT_DEC_TYPE_CODE = "MC80528";
    private static final String APP_STATUS = "DRAFT";

    @Before
    public void setUp() throws SQLException {
        caseId = randomUUID();
        offenceId = randomUUID();
        sessionId = randomUUID();
        appId = randomUUID();

        databaseCleaner.cleanViewStore();

        stubFixedLists();
        stubAllResultDefinitions();
        stubQueryForVerdictTypes();
        stubQueryForAllProsecutors();
        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        AssignmentStub.stubAssignmentReplicationCommands();
        SchedulingStub.stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(USER, "ALL");
        stubGroupForUser(USER.getUserId(), "Legal Advisers");
        stubResultIds();

        final ImmutableMap<String, Boolean> features = ImmutableMap.of("amendReshare", true);
        FeatureStubber.stubFeaturesFor("sjp", features);

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

        setPleas(caseId, createCasePayloadBuilder.getDefendantBuilder().getId(), new PleaInfo(offenceId, GUILTY));

        startSession(sessionId, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, USER.getUserId());

        final Discharge discharge = createDischarge(null, createOffenceDecisionInformation(offenceId, FOUND_GUILTY), CONDITIONAL, new DischargePeriod(2, MONTH), new BigDecimal(230), null, false, null, null);
        discharge.getOffenceDecisionInformation().setPressRestrictable(false);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, USER, asList(discharge), financialImposition());

        eventListener
                .withMaxWaitTime(10000)
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .subscribe(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                .run(() -> saveDecision(decision));
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseCompleted caseCompleted = eventListener.popEventPayload(CaseCompleted.class);
        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseCompleted(caseId, caseCompleted);

        final Optional<JsonEnvelope> caseResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);

        verifyPublicHearingResultedForOffences(caseResulted);
    }

    @Test
    public void shouldRecordApplicationDecisionGranted() {
        eventListener.subscribe(SJP_EVENT_CASE_APP_RECORDED, SJP_EVENT_CASE_APP_STAT_DEC)
                .run(() -> createCaseApplication(USER.getUserId(), caseId, appId,
                        STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                        createCaseApplicationFile));

        final Optional<JsonEnvelope> caseApplicationRecordedEnv = eventListener.popEvent(SJP_EVENT_CASE_APP_RECORDED);
        assertThat(caseApplicationRecordedEnv.isPresent(), is(true));
        pollUntilCaseReady(caseId);

        final UUID sessionId2 = randomUUID();
        startSession(sessionId2, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId2, USER.getUserId());

        eventListener.subscribe(
                SJP_EVENT_APPLICATION_DECISION_SAVED,
                SJP_EVENT_APPLICATION_STATUS_CHANGED,
                SJP_EVENT_APPLICATION_DECISION_SET_ASIDE,
                PUBLIC_SJP_APPLICATION_DECISION_SET_ASIDE,
                PUBLIC_EVENTS_HEARING_HEARING_RESULTED
        ).run(() -> saveApplicationDecision(USER.getUserId(), caseId, appId, sessionId2,
                        true, false,null, null));

        final Optional<JsonEnvelope> applicationDecisionSavedEnv = eventListener.popEvent(SJP_EVENT_APPLICATION_DECISION_SAVED);
        assertThat(applicationDecisionSavedEnv.isPresent(), is(true));
        final Optional<JsonEnvelope> applicationStatusChangedEnv = eventListener.popEvent(SJP_EVENT_APPLICATION_STATUS_CHANGED);
        assertThat(applicationStatusChangedEnv.isPresent(), is(true));
        final Optional<JsonEnvelope> applicationSetAside = eventListener.popEvent(SJP_EVENT_APPLICATION_DECISION_SET_ASIDE);
        assertThat(applicationSetAside.isPresent(), is(true));

        final Optional<JsonEnvelope> publicApplicationSetAside = eventListener.popEvent(PUBLIC_SJP_APPLICATION_DECISION_SET_ASIDE);
        assertThat(publicApplicationSetAside.isPresent(), is(true));

        final Optional<JsonEnvelope> caseResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        verifyPublicHearingResultedForApplication(caseResulted);

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.caseDecisions[1].applicationDecision.granted", is(true)),
                withJsonPath("$.caseDecisions[1].applicationDecision.outOfTime", is(false)),
                withJsonPath("$.caseDecisions[1].applicationDecision.applicationType", is("STAT_DEC")),
                withJsonPath("$.caseDecisions[1].applicationDecision.previousFinalDecision", notNullValue()),
                withJsonPath("$.caseApplication.applicationStatus", is("STATUTORY_DECLARATION_GRANTED")),
                withJsonPath("$.setAside", is(true)),
                withJsonPath("$.completed", is(false)),
                withJsonPath("$.defendant.offences[0].completed", is(false)),
                withJsonPath("$.defendant.offences[0].hasFinalDecision", is(false)),
                withoutJsonPath("$.defendant.offences[0].plea"),
                withoutJsonPath("$.defendant.offences[0].pleaMethod"),
                withoutJsonPath("$.defendant.offences[0].pleaDate"),
                withoutJsonPath("$.defendant.offences[0].conviction"),
                withoutJsonPath("$.defendant.offences[0].convictionDate")
        ));

        pollUntilCaseReady(caseId);

        eventListener.subscribe(
                SJP_EVENT_PLEAS_SET
        ).run(() -> {
            final SetPleasPayloadBuilder setPleas = setPleasPayloadBuilder()
                    .welshHearing(false)
                    .withInterpreter(null, false);
            setPleas.withPlea(offenceId, createCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY);
            setPleasAsUser(caseId, setPleas.build(), USER.getUserId());
        });

        final Optional<JsonEnvelope> pleasSet = eventListener.popEvent(SJP_EVENT_PLEAS_SET);
        assertThat(pleasSet.isPresent(), is(true));

        final Dismiss dismiss = withDefaults(offenceId).build();
        final DecisionCommand decision = new DecisionCommand(sessionId2, caseId, null, USER, asList(dismiss), null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(() -> saveDecision(decision));
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseCompleted caseCompleted = eventListener.popEventPayload(CaseCompleted.class);
        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseCompleted(caseId, caseCompleted);

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.setAside", is(false)),
                withJsonPath("$.completed", is(true)),
                withJsonPath("$.defendant.offences[0].completed", is(true)),
                withJsonPath("$.defendant.offences[0].hasFinalDecision", is(true)),
                withJsonPath("$.defendant.offences[0].plea", is("NOT_GUILTY"))
        ));
    }

    @Test
    public void shouldRecordApplicationDecisionRefusal() {
        eventListener.subscribe(SJP_EVENT_CASE_APP_RECORDED, SJP_EVENT_CASE_APP_STAT_DEC)
                .run(() -> createCaseApplication(USER.getUserId(), caseId, appId,
                        STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A", DATE_RECEIVED, APP_STATUS,
                        createCaseApplicationFile));

        final Optional<JsonEnvelope> caseApplicationRecordedEnv = eventListener.popEvent(SJP_EVENT_CASE_APP_RECORDED);
        assertThat(caseApplicationRecordedEnv.isPresent(), is(true));
        pollUntilCaseReady(caseId);

        final UUID sessionId2 = randomUUID();
        startSession(sessionId2, USER.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId2, USER.getUserId());

        eventListener.subscribe(
                SJP_EVENT_APPLICATION_DECISION_SAVED,
                SJP_EVENT_APPLICATION_STATUS_CHANGED,
                SJP_EVENT_APPLICATION_DECISION_SET_ASIDE,
                PUBLIC_SJP_APPLICATION_DECISION_SET_ASIDE,
                SJP_EVENT_CASE_UNASSIGNED,
                SJP_EVENTS_CASE_COMPLETED,
                PUBLIC_EVENTS_HEARING_HEARING_RESULTED
        ).run(() -> saveApplicationDecision(USER.getUserId(), caseId, appId, sessionId2,
                        false, null,null, "Insufficient evidence"));

        final Optional<JsonEnvelope> applicationDecisionSavedEnv = eventListener.popEvent(SJP_EVENT_APPLICATION_DECISION_SAVED);
        assertThat(applicationDecisionSavedEnv.isPresent(), is(true));
        final Optional<JsonEnvelope> applicationStatusChangedEnv = eventListener.popEvent(SJP_EVENT_APPLICATION_STATUS_CHANGED);
        assertThat(applicationStatusChangedEnv.isPresent(), is(true));
        final Optional<JsonEnvelope> caseUnassignedEv = eventListener.popEvent(SJP_EVENT_CASE_UNASSIGNED);
        assertThat(caseUnassignedEv.isPresent(), is(true));
        final Optional<JsonEnvelope> caseCompletedEvent = eventListener.popEvent(SJP_EVENTS_CASE_COMPLETED);
        assertThat(caseCompletedEvent.isPresent(), is(true));

        final Optional<JsonEnvelope> caseResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        verifyPublicHearingResultedForApplication(caseResulted);

        final Optional<JsonEnvelope> applicationSetAside = eventListener.popEvent(SJP_EVENT_APPLICATION_DECISION_SET_ASIDE);
        assertThat(applicationSetAside.isPresent(), is(false));
        final Optional<JsonEnvelope> publicApplicationSetAside = eventListener.popEvent(PUBLIC_SJP_APPLICATION_DECISION_SET_ASIDE);
        assertThat(publicApplicationSetAside.isPresent(), is(false));

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.caseDecisions[1].applicationDecision.granted", is(false)),
                withJsonPath("$.caseDecisions[1].applicationDecision.rejectionReason", is("Insufficient evidence")),
                withJsonPath("$.caseDecisions[1].applicationDecision.applicationType", is("STAT_DEC")),
                withJsonPath("$.caseDecisions[1].applicationDecision.previousFinalDecision", notNullValue()),
                withJsonPath("$.caseDecisions[1].applicationDecision.previousFinalDecisionObject", notNullValue()),
                withJsonPath("$.caseApplication.applicationStatus", is("STATUTORY_DECLARATION_REFUSED")),
                withJsonPath("$.setAside", is(false)),
                withJsonPath("$.completed", is(true)),
                withJsonPath("$.assigned", is(false)),
                withJsonPath("$.defendant.offences[0].conviction", is("FOUND_GUILTY")),
                withJsonPath("$.defendant.offences[0].convictionDate", notNullValue()),
                withJsonPath("$.defendant.offences[0].completed", is(true)),
                withJsonPath("$.defendant.offences[0].hasFinalDecision", is(true))
        ));

        pollUntilCaseNotReady(caseId);


    }

    private FinancialImposition financialImposition() {
        return new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(120), null, new BigDecimal(32), null, null, true),
                new Payment(new BigDecimal(272), PAY_TO_COURT, "No information from defendant", null,
                        new PaymentTerms(false, null, new Installments(new BigDecimal(20), InstallmentPeriod.MONTHLY, LocalDate.now().plusDays(30))), (new CourtDetails(NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court"))
                ));
    }

    private void verifyPublicHearingResultedForOffences(final Optional<JsonEnvelope> caseResultedEnvelope) {
        assertThat(caseResultedEnvelope.isPresent(), Matchers.is(true));
        final JsonEnvelope envelope = caseResultedEnvelope.get();
        assertThat(envelope,
                jsonEnvelope(
                        metadata().withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED),
                        payload().isJson(Matchers.allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.sharedTime", is(notNullValue()))
                        ))));
    }

    private void verifyPublicHearingResultedForApplication(final Optional<JsonEnvelope> caseResultedEnvelope) {
        assertThat(caseResultedEnvelope.isPresent(), Matchers.is(true));
        final JsonEnvelope envelope = caseResultedEnvelope.get();
        assertThat(envelope,
                jsonEnvelope(
                        metadata().withName(PUBLIC_EVENTS_HEARING_HEARING_RESULTED),
                        payload().isJson(Matchers.allOf(
                                withJsonPath("$.hearing", notNullValue()),
                                withJsonPath("$.hearing.courtApplications", notNullValue()),
                                withJsonPath("$.sharedTime", is(notNullValue()))
                        ))));
    }

}
