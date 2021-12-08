package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.OffenceBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseAccountHelper.pollForCaseAccountNote;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseNotReadyInViewStore;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSaved;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubFixedLists;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypes;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubWithdrawalReasonsQuery;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.executeTimerJobs;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.pollUntilProcessExists;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.builders.FinancialImpositionBuilder.costsAndSurcharge;
import static uk.gov.moj.sjp.it.util.builders.FinancialImpositionBuilder.withDefaults;
import static uk.gov.moj.sjp.it.util.matchers.OffenceDecisionMatcher.adjourn;
import static uk.gov.moj.sjp.it.util.matchers.OffenceDecisionMatcher.offenceDecisionHaving;
import static uk.gov.moj.sjp.it.util.matchers.OffenceDecisionMatcher.pressRestriction;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.event.PaymentTermsChanged;
import uk.gov.moj.cpp.sjp.event.decision.DecisionRejected;
import uk.gov.moj.cpp.sjp.event.decision.DecisionResubmitted;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.builders.AdjournBuilder;
import uk.gov.moj.sjp.it.util.builders.DischargeBuilder;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;
import uk.gov.moj.sjp.it.util.builders.FinancialPenaltyBuilder;
import uk.gov.moj.sjp.it.util.builders.NoSeparatePenaltyBuilder;
import uk.gov.moj.sjp.it.util.builders.WithdrawBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class PressRestrictionsSaveDecisionIT extends BaseIntegrationTest {

    private static final String WITHDRAWAL_REASON = "Insufficient evidence";
    private static final CourtDetails COURT_DETAILS = new CourtDetails("1080", "Bedfordshire Magistrates' Court");
    private static final String CHILDS_NAME = "Robert Robertson";
    private final EventListener eventListener = new EventListener();
    private final User user = new User("John", "Smith", USER_ID);
    private UUID sessionId = randomUUID();
    private UUID caseId = randomUUID();
    private UUID systemUserId = randomUUID();
    private LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    protected static final String SJP = "sjp";
    public static final String PUBLIC_EVENTS_HEARING_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    public static final String PUBLIC_CASE_DECISION_RE_SUBMITTED = "public.sjp.events.case-decision-resubmitted";

    private static JsonObject startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType) {
        final JsonEnvelope session = startSession(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, sessionType).get();
        requestCaseAssignment(sessionId, USER_ID);
        return session.payloadAsJsonObject();
    }

    @BeforeClass
    public static void setupBeforeClass() {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of("amendReshare", true);
        FeatureStubber.stubFeaturesFor(SJP, features);
    }

    @Before
    public void setUp() throws Exception {
        new SjpDatabaseCleaner().cleanViewStore();
        stubStartSjpSessionCommand();
        stubGroupForUser(systemUserId, "System Users");
        stubProsecutorQuery(ProsecutingAuthority.TFL.name(), ProsecutingAuthority.TFL.getFullName(), randomUUID());
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubAllResultDefinitions();
        stubFixedLists();
        stubQueryForVerdictTypes();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(user, "ALL");
        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        stubResultIds();
    }

    @Test
    public void shouldSavePressRestrictionDecisions() {
        // Given
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().pressRestriction("Robert Robertson").build();
        final Discharge discharge = DischargeBuilder.withDefaults().pressRestriction("Robert Robertson").build();
        final Dismiss dismiss = DismissBuilder.withDefaults().pressRestriction("Robert Robertson").build();
        final Withdraw withdraw = WithdrawBuilder.withDefaults().pressRestriction("Robert Robertson").build();
        stubWithdrawalReasonsQuery(withdraw.getWithdrawalReasonId(), WITHDRAWAL_REASON);
        final NoSeparatePenalty noSeparatePenalty = NoSeparatePenaltyBuilder.withDefaults().pressRestriction("Robert Robertson").build();
        final List<OffenceDecision> offenceDecisions = asList(financialPenalty, discharge, dismiss, withdraw, noSeparatePenalty);
        final FinancialImposition financialImposition = withDefaults();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, financialImposition);
        createCase(offenceDecisions);
        stubWithdrawalReasonsQuery(withdraw.getWithdrawalReasonId(), WITHDRAWAL_REASON);
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        // When
        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);

        //after saving the decision the transfer of fine element is attached
        financialImposition.getPayment().setFineTransferredTo(COURT_DETAILS);
        //also the pressRestrictable flag is attached to all press restrictable OffenceDecisionInformation
        offenceDecisions.stream()
                .flatMap(offenceDecision -> offenceDecision.offenceDecisionInformationAsList().stream())
                .forEach(offenceDecisionInformation -> offenceDecisionInformation.setPressRestrictable(true));

        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseNotReadyInViewStore(caseId, USER_ID);
    }

    @Test
    public void shouldSavePressRestrictionDecisionsOnlyForPressRestrictionAppliedOffences() {
        // Given
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().build();
        final Dismiss dismiss = DismissBuilder.withDefaults().pressRestriction(CHILDS_NAME).build();
        final List<OffenceDecision> offenceDecisions = asList(financialPenalty, dismiss);
        final FinancialImposition financialImposition = withDefaults();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, financialImposition);
        createCase(offenceDecisions);
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        // When
        eventListener.subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);

        assertThat(decisionSaved.getOffenceDecisions(), containsInAnyOrder(
                offenceDecisionHaving(FINANCIAL_PENALTY, pressRestriction(financialPenalty.getPressRestriction())),
                offenceDecisionHaving(DISMISS, pressRestriction(dismiss.getPressRestriction()))
        ));
    }

    @Test
    public void shouldRaiseDecisionRejectedWheApplyingPressRestrictionToNonPressRestrictableOffences() {
        // Given
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().build();
        final Dismiss dismiss = DismissBuilder.withDefaults().pressRestriction(CHILDS_NAME).build();
        final List<OffenceDecision> offenceDecisions = asList(financialPenalty, dismiss);
        final FinancialImposition financialImposition = withDefaults();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, financialImposition);
        final CreateCasePayloadBuilder theCase = createCasePayload(asList(financialPenalty.getId(), dismiss.getId())).setOffencesPressRestrictable(false);
        createCase(theCase);
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        // When
        eventListener.subscribe(DecisionRejected.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then
        final DecisionRejected decisionRejected = eventListener.popEventPayload(DecisionRejected.class);
        assertThat(decisionRejected.getRejectionReasons(), containsInAnyOrder("Press restriction cannot be applied to non-press-restrictable offence: " + dismiss.getId()));
    }

    @Test
    public void shouldSavePressRestrictionWhenCaseIsAdjournedWithMixedRestrictableAndNonRestrictableOffences() {
        // Given
        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(NO_VERDICT)
                .addOffenceDecisionInformation(NO_VERDICT)
                .pressRestriction(CHILDS_NAME).build();
        final List<OffenceDecision> offenceDecisions = singletonList(adjourn);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, null);
        final CreateCasePayloadBuilder casePayload = createCasePayload(adjourn.getOffenceIds());
        casePayload.getOffenceBuilders().get(0).withPressRestrictable(true);
        casePayload.getOffenceBuilders().get(1).withPressRestrictable(false);
        createCase(casePayload);
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        // When
        eventListener.subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);

        assertThat(decisionSaved.getOffenceDecisions(), containsInAnyOrder(
                offenceDecisionHaving(ADJOURN, pressRestriction(adjourn.getPressRestriction()))
        ));
    }

    @Test
    public void shouldSavePressRestrictionAfterCaseHasBeenAdjourned() {
        // Given
        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(NO_VERDICT)
                .addOffenceDecisionInformation(NO_VERDICT).build();
        final List<OffenceDecision> offenceDecisions = singletonList(adjourn);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, null);
        createCase(createCasePayload(adjourn.getOffenceIds()));
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        // When
        eventListener.subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        assertThat(decisionSaved.getOffenceDecisions(), hasSize(1));
        assertThat(decisionSaved.getOffenceDecisions().get(0).getPressRestriction(), nullValue());

        // Given a second session after adjournment
        final Withdraw withdraw = WithdrawBuilder.withDefaults()
                .id(adjourn.getOffenceIds().get(0))
                .pressRestriction(CHILDS_NAME).build();
        final Dismiss dismiss = DismissBuilder.withDefaults()
                .id(adjourn.getOffenceIds().get(1))
                .pressRestriction(CHILDS_NAME).build();

        final UUID secondSessionId = forceCaseBackIntoSession(caseId);
        final List<OffenceDecision> secondOffenceDecisions = asList(withdraw, dismiss);
        final DecisionCommand secondDecision = new DecisionCommand(secondSessionId, caseId, null, user, secondOffenceDecisions, null);

        // When saving a second decision
        eventListener.subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(secondDecision));

        // Then press restriction is saved
        final DecisionSaved secondDecisionSaved = eventListener.popEventPayload(DecisionSaved.class);

        assertThat(secondDecisionSaved.getOffenceDecisions(), containsInAnyOrder(
                offenceDecisionHaving(WITHDRAW, pressRestriction(withdraw.getPressRestriction())),
                offenceDecisionHaving(DISMISS, pressRestriction(dismiss.getPressRestriction()))
        ));
    }

    @Test
    public void shouldSavePressRestrictionOnAdjournmentAndMaintainRestrictionOnSecondDecision() {
        // Given
        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(NO_VERDICT)
                .addOffenceDecisionInformation(NO_VERDICT)
                .pressRestriction(CHILDS_NAME).build();
        final List<OffenceDecision> offenceDecisions = singletonList(adjourn);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, null);
        createCase(createCasePayload(adjourn.getOffenceIds()));
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        // When
        eventListener.subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        assertThat(decisionSaved.getOffenceDecisions(), containsInAnyOrder(
                adjourn(pressRestriction(adjourn.getPressRestriction())))
        );

        // Given a second session after adjournment
        final Withdraw withdraw = WithdrawBuilder.withDefaults()
                .id(adjourn.getOffenceIds().get(0))
                .pressRestriction(CHILDS_NAME).build();
        final Dismiss dismiss = DismissBuilder.withDefaults()
                .id(adjourn.getOffenceIds().get(1))
                .pressRestriction(CHILDS_NAME).build();

        final UUID secondSessionId = forceCaseBackIntoSession(caseId);
        final List<OffenceDecision> secondOffenceDecisions = asList(withdraw, dismiss);
        final DecisionCommand secondDecision = new DecisionCommand(secondSessionId, caseId, null, user, secondOffenceDecisions, null);

        // When saving a second decision
        eventListener.subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(secondDecision));

        // Then press restriction is saved
        final DecisionSaved secondDecisionSaved = eventListener.popEventPayload(DecisionSaved.class);

        assertThat(secondDecisionSaved.getOffenceDecisions(), containsInAnyOrder(
                offenceDecisionHaving(WITHDRAW, pressRestriction(withdraw.getPressRestriction())),
                offenceDecisionHaving(DISMISS, pressRestriction(dismiss.getPressRestriction()))
        ));
    }

    @Test
    public void shouldRevokePressRestriction() {
        // Given
        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(NO_VERDICT)
                .addOffenceDecisionInformation(NO_VERDICT)
                .pressRestriction(CHILDS_NAME).build();
        final List<OffenceDecision> offenceDecisions = singletonList(adjourn);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, null);
        createCase(createCasePayload(adjourn.getOffenceIds()));
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        // When
        eventListener.subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        assertThat(decisionSaved.getOffenceDecisions(), containsInAnyOrder(
                offenceDecisionHaving(ADJOURN, pressRestriction(adjourn.getPressRestriction())))
        );

        // Given a second session after adjournment
        final Withdraw withdraw = WithdrawBuilder.withDefaults()
                .id(adjourn.getOffenceIds().get(0))
                .pressRestrictionRevoked().build();
        final Dismiss dismiss = DismissBuilder.withDefaults()
                .id(adjourn.getOffenceIds().get(1))
                .pressRestrictionRevoked().build();

        final UUID secondSessionId = forceCaseBackIntoSession(caseId);
        final List<OffenceDecision> secondOffenceDecisions = asList(withdraw, dismiss);
        final DecisionCommand secondDecision = new DecisionCommand(secondSessionId, caseId, null, user, secondOffenceDecisions, null);

        // When saving a second decision
        eventListener.subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(secondDecision));

        // Then press restriction is saved
        final DecisionSaved secondDecisionSaved = eventListener.popEventPayload(DecisionSaved.class);

        assertThat(secondDecisionSaved.getOffenceDecisions(), containsInAnyOrder(
                offenceDecisionHaving(WITHDRAW, pressRestriction(PressRestriction.revoked())),
                offenceDecisionHaving(DISMISS, pressRestriction(PressRestriction.revoked()))
        ));
    }

    @Test
    public void shouldSavePaymentTerms() {
        // Given
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().build();
        final Dismiss dismiss = DismissBuilder.withDefaults().pressRestriction(CHILDS_NAME).build();
        final List<OffenceDecision> offenceDecisions = asList(financialPenalty, dismiss);
        final FinancialImposition financialImposition =
                new FinancialImposition(costsAndSurcharge(), new Payment(
                        new BigDecimal(370),
                        PAY_TO_COURT,
                        "Reason for not attached",
                        null,
                        new PaymentTerms(
                                false,
                                new LumpSum(
                                        new BigDecimal(370),
                                        14,
                                        LocalDate.of(2019, 7, 24)
                                ), null
                        ),
                        null
                ));
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, financialImposition);
        final CreateCasePayloadBuilder builder = createCase(offenceDecisions);
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        // When
        eventListener
                .withMaxWaitTime(10000)
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                .run(() -> DecisionHelper.saveDecision(decision));

        // Then
        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        assertThat(decisionSaved.getOffenceDecisions(), containsInAnyOrder(
                offenceDecisionHaving(FINANCIAL_PENALTY, pressRestriction(financialPenalty.getPressRestriction())),
                offenceDecisionHaving(DISMISS, pressRestriction(dismiss.getPressRestriction()))
        ));

        Optional<JsonEnvelope> publicHearingResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        assertThat(publicHearingResulted.orElseGet(null), notNullValue());

        // When
        eventListener
                .withMaxWaitTime(10000)
                .subscribe(DecisionResubmitted.EVENT_NAME)
                .subscribe(PUBLIC_EVENTS_HEARING_HEARING_RESULTED)
                .subscribe(PUBLIC_CASE_DECISION_RE_SUBMITTED)
                .subscribe(PaymentTermsChanged.EVENT_NAME)
                .run(() -> DecisionHelper.changePaymentTerms(user.getUserId(), caseId));

        final DecisionResubmitted decisionSavedWithPaymentTermsChanged
                = eventListener.popEventPayload(DecisionResubmitted.class);
        assertThat(decisionSavedWithPaymentTermsChanged, notNullValue());

        publicHearingResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        assertThat(publicHearingResulted.orElseGet(null), notNullValue());

        final Optional<JsonEnvelope> publicCaseDecisionReSubmitted = eventListener.popEvent(PUBLIC_CASE_DECISION_RE_SUBMITTED);
        assertThat(publicCaseDecisionReSubmitted.orElseGet(null), notNullValue());

        final JsonObject jsonObject = pollForCaseAccountNote(builder.getUrn(), withJsonPath("$.noteText", is(notNullValue())), systemUserId);
        assertThat(jsonObject.getString("noteText"), is(equalTo("PAYMENT TERMS HAVE BEEN RESET")));

    }

    private CreateCasePayloadBuilder createCase(final List<OffenceDecision> offenceDecisions) {
        final List<UUID> offenceIds = offenceDecisions.stream().map(OffenceDecision::getId).collect(toList());
        return createCase(createCasePayload(offenceIds));
    }

    private CreateCasePayloadBuilder createCase(final CreateCasePayloadBuilder casePayload) {
        final ProsecutingAuthority prosecutingAuthority = casePayload.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(casePayload.getDefendantBuilder().getAddressBuilder().getPostcode(),
                COURT_DETAILS.getNationalCourtCode(), COURT_DETAILS.getNationalCourtName());
        stubRegionByPostcode(COURT_DETAILS.getNationalCourtCode(), "TestRegion");

        createCaseForPayloadBuilder(casePayload);
        pollUntilCaseReady(casePayload.getId());
        return casePayload;
    }

    private CreateCasePayloadBuilder createCasePayload(final List<UUID> offenceIds) {
        final CreateCasePayloadBuilder createCasePayloadBuilder = CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withPostingDate(postingDate);

        final List<OffenceBuilder> offenceBuilders = offenceIds.stream()
                .map(this::createOffencePayload)
                .collect(toList());

        createCasePayloadBuilder.withOffenceBuilders(offenceBuilders);
        return createCasePayloadBuilder;
    }

    private OffenceBuilder createOffencePayload(final UUID offenceId) {
        return OffenceBuilder.withDefaults()
                .withId(offenceId)
                .withPressRestrictable(true);
    }

    private UUID forceCaseBackIntoSession(final UUID caseId) {
        final String pendingAdjournmentProcess = pollUntilProcessExists("timerTimeout", caseId.toString());
        executeTimerJobs(pendingAdjournmentProcess);
        pollUntilCaseReady(caseId);
        final UUID sessionId = randomUUID();
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);
        return sessionId;
    }
}
