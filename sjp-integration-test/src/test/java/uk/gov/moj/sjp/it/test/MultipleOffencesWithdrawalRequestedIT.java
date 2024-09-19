package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.sjp.it.Constants.EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.defaultCaseBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.OffenceBuilder.defaultOffenceBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseIsReadyInViewStore;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseNotReadyInViewStore;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseUnmarkedReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSaved;
import static uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper.assertCaseQueryDoesNotReturnWithdrawalReasons;
import static uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper.assertCaseQueryReturnsWithdrawalReasons;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubWithdrawalReasonsQuery;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.EventUtil.eventsByName;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestReasonChanged;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("DD-17905:Commented as part of 21.25.01 as it is failing on Jenkins at random, but works locally")
public class MultipleOffencesWithdrawalRequestedIT extends BaseIntegrationTest {

    private final UUID withdrawalRequestReasonId1 = randomUUID();
    private final UUID withdrawalRequestReasonId2 = randomUUID();
    private final Map<UUID, String> withdrawalReasons = ImmutableMap.of(withdrawalRequestReasonId1, "Insufficient Evidence", withdrawalRequestReasonId2, "Not in public interest to proceed");
    private final UUID userId = USER_ID;
    private final User user = new User("John", "Smith", userId);
    private final UUID caseId = randomUUID();
    private final UUID offenceId1 = randomUUID();
    private final UUID offenceId2 = randomUUID();

    private static final String ADJOURN_REASON = "Not enough documents present for decision, waiting for document";
    private CreateCase.CreateCasePayloadBuilder aCase;
    private static final String NATIONAL_COURT_CODE = "1080";

    @BeforeEach
    public void setUp() throws Exception {
        new SjpDatabaseCleaner().cleanViewStore();

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        aCase = defaultCaseBuilder().withId(caseId)
                .withOffenceBuilders(
                        defaultOffenceBuilder().withId(offenceId1),
                        defaultOffenceBuilder().withId(offenceId2));
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");

        final ProsecutingAuthority prosecutingAuthority = aCase.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(aCase))
                .popEvent(CaseReceived.EVENT_NAME);

        stubWithdrawalReasonsQuery(withdrawalReasons);
    }

    @Test
    public void offenceWithdrawalRequestForMultipleOffences() throws Exception {
        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET, OffenceWithdrawalRequested.EVENT_NAME)) {

            withdrawalHelper.requestWithdrawalOfOffences(caseId, requestPayload());

            final Map<String, List<JsonEnvelope>> privateEventsByName = eventsByName(
                    withdrawalHelper.getEventFromTopic(),
                    withdrawalHelper.getEventFromTopic(),
                    withdrawalHelper.getEventFromTopic());

            final Map<String, JsonEnvelope> withdrawalRequestedEventsByOffenceId = privateEventsByName.get(OffenceWithdrawalRequested.EVENT_NAME).stream()
                    .collect(toMap(event -> event.payloadAsJsonObject().getString("offenceId"), identity()));

            final JsonEnvelope offencesWithdrawalStatusSetPublicEvent = withdrawalHelper.getEventFromPublicTopic();
            final JsonEnvelope offencesWithdrawalStatusSetPrivateEvent = privateEventsByName.get(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET).get(0);
            final JsonEnvelope offence1WithdrawalRequestedPrivateEvent = withdrawalRequestedEventsByOffenceId.get(offenceId1.toString());
            final JsonEnvelope offence2WithdrawalRequestedPrivateEvent = withdrawalRequestedEventsByOffenceId.get(offenceId2.toString());

            final Matcher offencesWithdrawalStatusSetPayloadMatcher = allOf(
                    withJsonPath("$.caseId", equalTo(caseId.toString())),
                    withJsonPath("$.setAt", notNullValue()),
                    withJsonPath("$.setBy", equalTo(userId.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].offenceId", equalTo(offenceId1.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[1].offenceId", equalTo(offenceId2.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[1].withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString())),
                    withJsonPath("$.withdrawalRequestsStatus.length()", equalTo(2)));

            assertThat(offencesWithdrawalStatusSetPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offencesWithdrawalStatusSetPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offence1WithdrawalRequestedPrivateEvent, jsonEnvelope(
                    metadata().withName(OffenceWithdrawalRequested.EVENT_NAME),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.offenceId", equalTo(offenceId1.toString())),
                            withJsonPath("$.withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString())),
                            withJsonPath("$.requestedBy", equalTo(userId.toString())),
                            withJsonPath("$.requestedAt", notNullValue())
                    )))));

            assertThat(offence2WithdrawalRequestedPrivateEvent, jsonEnvelope(
                    metadata().withName(OffenceWithdrawalRequested.EVENT_NAME),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.offenceId", equalTo(offenceId2.toString())),
                            withJsonPath("$.withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString())),
                            withJsonPath("$.requestedBy", equalTo(userId.toString())),
                            withJsonPath("$.requestedAt", notNullValue())
                    )))));

            assertCaseQueryReturnsWithdrawalReasons(caseId, requestPayload(), withdrawalReasons);
        }
    }

    @Test
    @Disabled("DD-17905:Commented as part of 21.25.01 as it is failing on Jenkins at random, but works locally")
    public void offenceWithdrawalForSomeOffencesOnAPartiallyDecidedCaseMakesCaseReady() throws Exception {
        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(user, "ALL");
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");



        final UUID sessionId = randomUUID();

        startSessionAndRequestAssignment(sessionId, userId, MAGISTRATE);

        final Withdraw withdrawDecision = new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), randomUUID());
        final Adjourn adjournDecision = new Adjourn(randomUUID(), singletonList(createOffenceDecisionInformation(offenceId2, NO_VERDICT)), ADJOURN_REASON, now().plusDays(10));

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, asList(withdrawDecision, adjournDecision), null);

        final EventListener eventListener = new EventListener();
        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .subscribe(CaseUnmarkedReadyForDecision.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);
        final CaseUnmarkedReadyForDecision caseUnmarkedReadyForDecision = eventListener.popEventPayload(CaseUnmarkedReadyForDecision.class);

        decision.getOffenceDecisions().stream()
                .flatMap(offenceDecision -> offenceDecision.offenceDecisionInformationAsList().stream())
                .forEach(offDcnInfo -> offDcnInfo.setPressRestrictable(false));
        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseUnmarkedReady(caseId, adjournDecision, caseUnmarkedReadyForDecision);

        verifyCaseNotReadyInViewStore(caseId, userId);

        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET, OffenceWithdrawalRequested.EVENT_NAME)) {
            withdrawalHelper.requestWithdrawalOfOffences(caseId, requestPayload(singletonList(offenceId2)));

            final Map<String, List<JsonEnvelope>> privateEventsByName = eventsByName(
                    withdrawalHelper.getEventFromTopic(),
                    withdrawalHelper.getEventFromTopic());

            final Map<String, JsonEnvelope> withdrawalRequestedEventsByOffenceId = privateEventsByName
                    .get(OffenceWithdrawalRequested.EVENT_NAME).stream()
                    .collect(toMap(event -> event.payloadAsJsonObject().getString("offenceId"), identity()));

            final JsonEnvelope offence2WithdrawalRequestedPrivateEvent = withdrawalRequestedEventsByOffenceId.get(offenceId2.toString());

            assertThat(offence2WithdrawalRequestedPrivateEvent, jsonEnvelope(
                    metadata().withName(OffenceWithdrawalRequested.EVENT_NAME),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.offenceId", equalTo(offenceId2.toString())),
                            withJsonPath("$.withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString())),
                            withJsonPath("$.requestedBy", equalTo(userId.toString())),
                            withJsonPath("$.requestedAt", notNullValue())
                    )))));

            verifyCaseIsReadyInViewStore(caseId, userId);

        }

    }

    private static JsonObject startSessionAndRequestAssignment(final UUID sessionId, final UUID userId, final SessionType sessionType) {
        final JsonEnvelope session = startSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, sessionType).get();
        requestCaseAssignment(sessionId, userId);
        return session.payloadAsJsonObject();
    }

    @Test
    public void offenceWithdrawalRequestCancelledForMultipleOffences() throws Exception {
        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET)) {
            withdrawalHelper.requestWithdrawalOfOffences(caseId, requestPayload());
            assertThat(withdrawalHelper.getEventFromPublicTopic(), notNullValue());
            assertThat(withdrawalHelper.getEventFromTopic(), notNullValue());
        }

        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET, OffenceWithdrawalRequestCancelled.EVENT_NAME)) {
            withdrawalHelper.requestWithdrawalOfOffences(caseId, cancelRequestPayload());

            final Map<String, List<JsonEnvelope>> privateEventsByName = eventsByName(
                    withdrawalHelper.getEventFromTopic(),
                    withdrawalHelper.getEventFromTopic());

            final JsonEnvelope offencesWithdrawalStatusSetPublicEvent = withdrawalHelper.getEventFromPublicTopic();
            final JsonEnvelope offencesWithdrawalStatusSetPrivateEvent = privateEventsByName.get(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET).get(0);
            final JsonEnvelope offencesWithdrawalRequestCancelledPrivateEvent = privateEventsByName.get(OffenceWithdrawalRequestCancelled.EVENT_NAME).get(0);

            final Matcher offencesWithdrawalStatusSetPayloadMatcher = allOf(
                    withJsonPath("$.caseId", equalTo(caseId.toString())),
                    withJsonPath("$.setAt", notNullValue()),
                    withJsonPath("$.setBy", equalTo(userId.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].offenceId", equalTo(offenceId2.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString())),
                    withJsonPath("$.withdrawalRequestsStatus.length()", equalTo(1)));

            assertThat(offencesWithdrawalStatusSetPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offencesWithdrawalStatusSetPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offencesWithdrawalRequestCancelledPrivateEvent, jsonEnvelope(
                    metadata().withName(OffenceWithdrawalRequestCancelled.EVENT_NAME),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.offenceId", equalTo(offenceId1.toString())),
                            withJsonPath("$.cancelledBy", equalTo(userId.toString())),
                            withJsonPath("$.cancelledAt", notNullValue())
                    )))));

            assertCaseQueryDoesNotReturnWithdrawalReasons(caseId, offenceId1);
        }
    }

    @Test
    public void offenceWithdrawalRequestReasonChangeForMultipleOffences() throws Exception {
        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET)) {
            withdrawalHelper.requestWithdrawalOfOffences(caseId, requestPayload());
            assertThat(withdrawalHelper.getEventFromTopic(), notNullValue());
            assertThat(withdrawalHelper.getEventFromPublicTopic(), notNullValue());
        }

        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET, OffenceWithdrawalRequestReasonChanged.EVENT_NAME)) {
            withdrawalHelper.requestWithdrawalOfOffences(caseId, requestForReasonChangePayload());

            final Map<String, List<JsonEnvelope>> privateEventsByName = eventsByName(
                    withdrawalHelper.getEventFromTopic(),
                    withdrawalHelper.getEventFromTopic());

            final JsonEnvelope offencesWithdrawalStatusSetPublicEvent = withdrawalHelper.getEventFromPublicTopic();
            final JsonEnvelope offencesWithdrawalStatusSetPrivateEvent = privateEventsByName.get(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET).get(0);
            final JsonEnvelope offencesWithdrawalReasonChangedPrivateEvent = privateEventsByName.get(OffenceWithdrawalRequestReasonChanged.EVENT_NAME).get(0);

            final Matcher offencesWithdrawalStatusSetPayloadMatcher = allOf(
                    withJsonPath("$.caseId", equalTo(caseId.toString())),
                    withJsonPath("$.setAt", notNullValue()),
                    withJsonPath("$.setBy", equalTo(userId.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].offenceId", equalTo(offenceId1.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[1].offenceId", equalTo(offenceId2.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[1].withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId2.toString())),
                    withJsonPath("$.withdrawalRequestsStatus.length()", equalTo(2)));

            assertThat(offencesWithdrawalStatusSetPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offencesWithdrawalStatusSetPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offencesWithdrawalReasonChangedPrivateEvent, jsonEnvelope(
                    metadata().withName(OffenceWithdrawalRequestReasonChanged.EVENT_NAME),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.offenceId", equalTo(offenceId2.toString())),
                            withJsonPath("$.changedBy", equalTo(userId.toString())),
                            withJsonPath("$.changedAt", notNullValue()),
                            withJsonPath("$.newWithdrawalRequestReasonId", equalTo(withdrawalRequestReasonId2.toString())),
                            withJsonPath("$.oldWithdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString()))
                    )))));

            assertCaseQueryReturnsWithdrawalReasons(caseId, requestForReasonChangePayload());
        }
    }

    private List<WithdrawalRequestsStatus> requestPayload() {
        final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses = new ArrayList<>();
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offenceId1, withdrawalRequestReasonId1));
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offenceId2, withdrawalRequestReasonId1));
        return withdrawalRequestsStatuses;
    }

    private List<WithdrawalRequestsStatus> requestPayload(Collection<UUID> offenceIds){
        return offenceIds.stream()
                .map(offenceId -> new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId1))
                .collect(Collectors.toList());
    }

    private List<WithdrawalRequestsStatus> cancelRequestPayload() {
        final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses = new ArrayList<>();
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offenceId2, withdrawalRequestReasonId1));
        return withdrawalRequestsStatuses;
    }

    private List<WithdrawalRequestsStatus> requestForReasonChangePayload() {
        final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses = new ArrayList<>();
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offenceId1, withdrawalRequestReasonId1));
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offenceId2, withdrawalRequestReasonId2));
        return withdrawalRequestsStatuses;
    }
}
