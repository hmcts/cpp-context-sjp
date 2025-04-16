package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAsync;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.requestSetPleasAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER_ID;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.commandclient.AssignNextCaseClient;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 class AssignmentRulesIT extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AssignmentRulesIT.class);

    private static final String DATE_TO_AVOID = "a-date-to-avoid";

    private final UUID withdrawalRequestReasonId = randomUUID();

    private CreateCase.CreateCasePayloadBuilder tflPiaCasePayloadBuilder, tflOldPiaCasePayloadBuilder, tflPleadedGuiltyCasePayloadBuilder, tflPleadedNotGuiltyCasePayloadBuilder, tflPendingWithdrawalCasePayloadBuilder,
            tvlPiaCasePayloadBuilder, tvlPleadedGuiltyRequestHearingCasePayloadBuilder, dvlaPiaCasePayloadBuilder, dvlaPleadedNotGuiltyCasePayloadBuilder;

    private final UUID userId = DEFAULT_USER_ID;

    @BeforeEach
    public void setUp() throws Exception {
        cleanViewStore();

        stubDefaultCourtByCourtHouseOUCodeQuery();

        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
        stubProsecutorQuery(TVL.name(), TVL.getFullName(), randomUUID());
        stubProsecutorQuery(DVLA.name(), DVLA.getFullName(), randomUUID());
        stubForUserDetails(userId, "ALL");
        setupIdMapperStub();

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        final String defaultPostcodeUsed = withDefaults().getDefendantBuilder().getAddressBuilder().getPostcode();

        tflPiaCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(31));

        tflOldPiaCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(32));

        tflPleadedGuiltyCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(10));

        tflPleadedNotGuiltyCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(11));

        tflPendingWithdrawalCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(5));

        tvlPiaCasePayloadBuilder =
                withDefaults()
                        .withPostingDate(daysAgo(30))
                        .withProsecutingAuthority(TVL);

        tvlPleadedGuiltyRequestHearingCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(10))
                .withProsecutingAuthority(TVL);

        dvlaPiaCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(33))
                .withProsecutingAuthority(DVLA);

        dvlaPleadedNotGuiltyCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(5))
                .withProsecutingAuthority(DVLA);


        final List<CreateCase.CreateCasePayloadBuilder> caseHelpers = asList(
                tflPleadedGuiltyCasePayloadBuilder,
                tflPleadedNotGuiltyCasePayloadBuilder,
                tflPendingWithdrawalCasePayloadBuilder,
                tvlPleadedGuiltyRequestHearingCasePayloadBuilder,
                dvlaPleadedNotGuiltyCasePayloadBuilder,
                dvlaPiaCasePayloadBuilder,
                tflPiaCasePayloadBuilder,
                tflOldPiaCasePayloadBuilder,
                tvlPiaCasePayloadBuilder);

        stubEnforcementAreaByPostcode(defaultPostcodeUsed, "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        caseHelpers.forEach(helper -> {
            stubGetEmptyAssignmentsByDomainObjectId(helper.getId());
        });

        caseHelpers.forEach(CreateCase::createCaseForPayloadBuilder);

        // pleaded guilty case
        requestSetPleasAndConfirm(tflPleadedGuiltyCasePayloadBuilder.getId(),
                true,
                false,
                true,
                null,
                false,
                null,
                asList(Triple.of(tflPleadedGuiltyCasePayloadBuilder.getOffenceId(),
                        tflPleadedGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), GUILTY)));

        // pleaded not guilty case
        requestSetPleasAndConfirm(tflPleadedNotGuiltyCasePayloadBuilder.getId(),
                true,
                false,
                true,
                null,
                false,
                null,
                asList(Triple.of(tflPleadedNotGuiltyCasePayloadBuilder.getOffenceId(),
                        tflPleadedNotGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY)));

        // pleaded guilty request hearing case
        requestSetPleasAndConfirm(tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId(),
                true,
                false,
                true,
                null,
                false,
                null,
                asList(Triple.of(tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getOffenceId(),
                        tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getDefendantBuilder().getId(), GUILTY_REQUEST_HEARING)));

        // dvla not guilty
        requestSetPleasAndConfirm(dvlaPleadedNotGuiltyCasePayloadBuilder.getId(),
                true,
                false,
                true,
                null,
                false,
                null,
                asList(Triple.of(dvlaPleadedNotGuiltyCasePayloadBuilder.getOffenceId(), dvlaPleadedNotGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY)));


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
     void londonCourtsCanHandleBothTflAndTvlCases() {
         List<CompletableFuture<Void>> futures = List.of(
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromMagistrateSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflPleadedGuiltyCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromMagistrateSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, dvlaPiaCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromMagistrateSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflOldPiaCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromMagistrateSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflPiaCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromMagistrateSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tvlPiaCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflPendingWithdrawalCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tflPleadedNotGuiltyCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId()))
         );

         // Wait for all tasks to complete
         CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
     }

     @Test
     void nonLondonCourtsShouldHandleOnlyNonTflCases() {
         List<CompletableFuture<Void>> futures = List.of(
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromMagistrateSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE, dvlaPiaCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromMagistrateSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE, tvlPiaCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE, tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId())),
                 CompletableFuture.runAsync(() -> verifyCaseAssignedFromDelegatedPowersSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE, dvlaPleadedNotGuiltyCasePayloadBuilder.getId()))
         );

         // Wait for all tasks to complete
         CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
         verifyCaseNotFoundInDelegatedPowersSession(DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE);
     }

    @Test
    void shouldHandleConcurrentAssignmentRequestFromMultipleLegalAdvisers() {
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

        SessionHelper.startSessionAndConfirm(sessionId, userId, courtHouseOUCode, sessionType);

        requestCaseAssignmentAndConfirm(sessionId, userId, caseId);
        pollUntilCaseAssignedToUser(caseId, userId);
    }

    private static void verifyCaseNotFound(final String courtHouseOUCode, final SessionType sessionType) {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        SessionHelper.startSessionAndConfirm(sessionId, userId, courtHouseOUCode, sessionType);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.notAssignedHandler = (envelope) -> log.info("Case Not Assigned");
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
