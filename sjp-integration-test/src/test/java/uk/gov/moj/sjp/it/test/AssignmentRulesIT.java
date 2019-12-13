package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.DELEGATED_POWERS_DECISION;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SET_PLEAS;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAsync;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.commandclient.AssignNextCaseClient;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.helper.SetPleasHelper;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.log.Log;

public class AssignmentRulesIT extends BaseIntegrationTest {

    private static final String DATE_TO_AVOID = "a-date-to-avoid";

    private final UUID withdrawalRequestReasonId = randomUUID();

    private EventListener eventListener = new EventListener();

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private CreateCase.CreateCasePayloadBuilder tflPiaCasePayloadBuilder, tflOldPiaCasePayloadBuilder, tflPleadedGuiltyCasePayloadBuilder, tflPleadedNotGuiltyCasePayloadBuilder, tflPendingWithdrawalCasePayloadBuilder,
            tvlPiaCasePayloadBuilder, tvlPleadedGuiltyRequestHearingCasePayloadBuilder, dvlaPiaCasePayloadBuilder, dvlaPleadedNotGuiltyCasePayloadBuilder;

    private UUID userId;

    @Before
    public void setUp() throws Exception {
        final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
        databaseCleaner.cleanAll();

        ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery();
        AssignmentStub.stubAssignmentReplicationCommands();
        SchedulingStub.stubStartSjpSessionCommand();

        tflPiaCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(31));

        tflOldPiaCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(32));

        tflPleadedGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(10));

        tflPleadedNotGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(11));

        tflPendingWithdrawalCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(5));

        tvlPiaCasePayloadBuilder =
                CreateCase.CreateCasePayloadBuilder.withDefaults()
                        .withPostingDate(daysAgo(30))
                        .withProsecutingAuthority(ProsecutingAuthority.TVL);

        tvlPleadedGuiltyRequestHearingCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(10))
                .withProsecutingAuthority(ProsecutingAuthority.TVL);

        dvlaPiaCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(33))
                .withProsecutingAuthority(ProsecutingAuthority.DVLA);

        dvlaPleadedNotGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(5))
                .withProsecutingAuthority(ProsecutingAuthority.DVLA);

        this.databaseCleaner.cleanAll();

        final List<CreateCase.CreateCasePayloadBuilder> caseHelpers = asList(
                tflPiaCasePayloadBuilder,
                tflOldPiaCasePayloadBuilder,
                tflPleadedGuiltyCasePayloadBuilder,
                tflPleadedNotGuiltyCasePayloadBuilder,
                tflPendingWithdrawalCasePayloadBuilder,
                tvlPleadedGuiltyRequestHearingCasePayloadBuilder,
                tvlPiaCasePayloadBuilder,
                dvlaPleadedNotGuiltyCasePayloadBuilder,
                dvlaPiaCasePayloadBuilder);

        caseHelpers.forEach(helper -> {
            stubGetEmptyAssignmentsByDomainObjectId(helper.getId());
        });

        caseHelpers.forEach(CreateCase::createCaseForPayloadBuilder);

        // pleaded guilty case
        SetPleasHelper.requestSetPleas(tflPleadedGuiltyCasePayloadBuilder.getId(),
                eventListener,
                true,
                false,
                true,
                null,
                false,
                asList(Triple.of(tflPleadedGuiltyCasePayloadBuilder.getOffenceId(),
                        tflPleadedGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), GUILTY)),
                PUBLIC_EVENT_SET_PLEAS);

        // pleaded not guilty case
        SetPleasHelper.requestSetPleas(tflPleadedNotGuiltyCasePayloadBuilder.getId(),
                eventListener,
                true,
                false,
                true,
                null,
                false,
                asList(Triple.of(tflPleadedNotGuiltyCasePayloadBuilder.getOffenceId(),
                        tflPleadedNotGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY)),
                PUBLIC_EVENT_SET_PLEAS);

        // pleaded guilty request hearing case
        SetPleasHelper.requestSetPleas(tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId(),
                eventListener,
                true,
                false,
                true,
                null,
                false,
                asList(Triple.of(tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getOffenceId(),
                        tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getDefendantBuilder().getId(), GUILTY_REQUEST_HEARING)),
                PUBLIC_EVENT_SET_PLEAS);

        // dvla not guilty
        SetPleasHelper.requestSetPleas(dvlaPleadedNotGuiltyCasePayloadBuilder.getId(),
                eventListener,
                true,
                false,
                true,
                null,
                false,
                asList(Triple.of(dvlaPleadedNotGuiltyCasePayloadBuilder.getOffenceId(), dvlaPleadedNotGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY)),
                PUBLIC_EVENT_SET_PLEAS);


        userId = randomUUID();

        OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET);
        offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(tflPendingWithdrawalCasePayloadBuilder.getId(), getRequestWithdrawalPayload(tflPendingWithdrawalCasePayloadBuilder.getOffenceBuilder().getId()));

        final Matcher offencesWithdrawalStatusSetPayloadMatcher = allOf(
                withJsonPath("$.caseId", Matchers.equalTo(tflPendingWithdrawalCasePayloadBuilder.getId().toString())),
                withJsonPath("$.setAt", Matchers.notNullValue()),
                withJsonPath("$.setBy", Matchers.equalTo(userId.toString())),
                withJsonPath("$.withdrawalRequestsStatus[0].offenceId", Matchers.equalTo(tflPendingWithdrawalCasePayloadBuilder.getOffenceId().toString())),
                withJsonPath("$.withdrawalRequestsStatus[0].withdrawalRequestReasonId", Matchers.equalTo(withdrawalRequestReasonId.toString())),
                withJsonPath("$.withdrawalRequestsStatus.length()", Matchers.equalTo(1)));

        final JsonEnvelope offencesWithdrawalStatusSetPublicEvent = offencesWithdrawalRequestHelper.getEventFromPublicTopic();
        assertThat(offencesWithdrawalStatusSetPublicEvent, jsonEnvelope(
                metadata().withName(PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

        addDatesToAvoid(tflPleadedNotGuiltyCasePayloadBuilder.getId(), DATE_TO_AVOID);
        addDatesToAvoid(dvlaPleadedNotGuiltyCasePayloadBuilder.getId(), DATE_TO_AVOID);
    }

    @Test
    public void londonCourtsCanHandleBothTflAndTvlCases() {
        verifyCaseAssignedFromMagistrateSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflPleadedGuiltyCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflOldPiaCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflPiaCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tvlPiaCasePayloadBuilder.getId());

        verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflPendingWithdrawalCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId());
    }

    @Test
    public void nonLondonCourtsShouldHandleOnlyNonTflCases() {
        verifyCaseAssignedFromMagistrateSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE, dvlaPiaCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE, tvlPiaCasePayloadBuilder.getId());

        verifyCaseNotFoundInMagistrateSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE);

        verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE, tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE, dvlaPleadedNotGuiltyCasePayloadBuilder.getId());

        verifyCaseNotFoundInDelegatedPowersSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE);
    }

    @Test
    public void shouldHandleConcurrentAssignmentRequestFromMultipleLegalAdvisers() {
        final Map<UUID, UUID> sessionIdByUserId = Stream.generate(UUID::randomUUID).limit(3).collect(toMap(identity(), la -> randomUUID()));

        sessionIdByUserId
                .entrySet()
                .parallelStream()
                .forEach(sessionByUser -> startSessionAsync(sessionByUser.getValue(), sessionByUser.getKey(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE));

        final Map<UUID, UUID> assignedCaseByUserId = sessionIdByUserId
                .entrySet()
                .parallelStream()
                .map(entry -> AssignmentHelper.requestCaseAssignment(entry.getValue(), entry.getKey()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(JsonEnvelope::payloadAsJsonObject)
                .collect(toMap(assignment -> UUID.fromString(assignment.getString("assigneeId")), assignment -> UUID.fromString(assignment.getString("caseId"))));

        assertThat(assignedCaseByUserId.keySet(), containsInAnyOrder(sessionIdByUserId.keySet().toArray()));
        assertThat(assignedCaseByUserId.values(), containsInAnyOrder(
                tflPiaCasePayloadBuilder.getId(),
                tflOldPiaCasePayloadBuilder.getId(),
                tflPleadedGuiltyCasePayloadBuilder.getId())
        );
    }

    private static void verifyCaseAssignedFromMagistrateSession(final String courtHouseOUCode, final UUID caseId) {
        verifyCaseAssigned(courtHouseOUCode, MAGISTRATE, caseId);
    }

    private static void verifyCaseAssignedFromDelegatedPowersSession(final String courtHouseOUCode, final UUID caseId) {
        verifyCaseAssigned(courtHouseOUCode, DELEGATED_POWERS, caseId);
    }

    private static void verifyCaseNotFoundInMagistrateSession(final String courtHouseOUCode) {
        verifyCaseNotFound(courtHouseOUCode, MAGISTRATE);
    }

    private static void verifyCaseNotFoundInDelegatedPowersSession(final String courtHouseOUCode) {
        verifyCaseNotFound(courtHouseOUCode, DELEGATED_POWERS);
    }

    private static void verifyCaseAssigned(final String courtHouseOUCode, final SessionType sessionType, final UUID caseId) {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        SessionHelper.startSession(sessionId, userId, courtHouseOUCode, sessionType);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.assignedPrivateHandler = (envelope) -> {
            assertThat((JsonEnvelope) envelope,
                    jsonEnvelope(
                            metadata().withName(CaseAssigned.EVENT_NAME),
                            payload().isJson(allOf(
                                    withJsonPath("$.caseId", equalTo(caseId.toString())),
                                    withJsonPath("$.assigneeId", equalTo(userId.toString())),
                                    withJsonPath("$.assignedAt", notNullValue()),
                                    withJsonPath("$.caseAssignmentType", equalTo(sessionType == MAGISTRATE ? MAGISTRATE_DECISION.toString() : DELEGATED_POWERS_DECISION.toString()))
                            ))));
        };
        assignCase.assignedPublicHandler = (envelope) -> {
            assertThat((JsonEnvelope) envelope,
                    jsonEnvelope(
                            metadata().withName(AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNED),
                            payload().isJson(withJsonPath("$.caseId", equalTo(caseId.toString())))));

        };
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        assertThat(AssignmentHelper.isCaseAssignedToUser(caseId, userId), is(true));
    }

    private static void verifyCaseNotFound(final String courtHouseOUCode, final SessionType sessionType) {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        SessionHelper.startSession(sessionId, userId, courtHouseOUCode, sessionType);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.notAssignedHandler = (envelope) -> Log.info("Case Not Assigned");
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();
    }

    private static LocalDate daysAgo(int days) {
        return LocalDate.now().minusDays(days);
    }

    private List<WithdrawalRequestsStatus> getRequestWithdrawalPayload(UUID offence1Id) {
        final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses = new ArrayList<>();
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offence1Id, withdrawalRequestReasonId));
        return withdrawalRequestsStatuses;
    }
}
