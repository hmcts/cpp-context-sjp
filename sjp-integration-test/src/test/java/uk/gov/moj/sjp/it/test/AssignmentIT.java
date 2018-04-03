package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.DELEGATED_POWERS_DECISION;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.EventSelector.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.stub.ReferenceDataStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AssignmentIT extends BaseIntegrationTest {

    private static final String GUILTY = "GUILTY", NOT_GUILTY = "NOT_GUILTY", GUILTY_REQUEST_HEARING = "GUILTY_REQUEST_HEARING";
    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572", WEST_MIDLANDS_LJA_NATIONAL_COURT_CODE = "2905", OTHER_LJA_NATIONAL_COURT_CODE = "3000";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK", WEST_MIDLANDS_COURT_HOUSE_OU_CODE = "B20EB", OTHER_COURT_HOUSE_OU_CODE = "B20YY";
    private static Map<String, String> ljaByCourtHouseOUCode;

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private CreateCase.CreateCasePayloadBuilder tflPiaCasePayloadBuilder, tflPleadedGuiltyCasePayloadBuilder, tflPleadedNotGuiltyCasePayloadBuilder, tflPendingWithdrawalCasePayloadBuilder,
            tvlPiaCasePayloadBuilder, tvlPleadedGuiltyRequestHearingCasePayloadBuilder, dvlaPiaCasePayloadBuilder, dvlaPleadedNotGuiltyCasePayloadBuilder;

    private AssignmentHelper assignmentHelper;

    @BeforeClass
    public static void init() {
        ljaByCourtHouseOUCode = new HashMap<>();
        ljaByCourtHouseOUCode.put(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);
        ljaByCourtHouseOUCode.put(WEST_MIDLANDS_COURT_HOUSE_OU_CODE, WEST_MIDLANDS_LJA_NATIONAL_COURT_CODE);
        ljaByCourtHouseOUCode.put(OTHER_COURT_HOUSE_OU_CODE, OTHER_LJA_NATIONAL_COURT_CODE);
    }

    @Before
    public void setUp() throws Exception {

        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);
        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(WEST_MIDLANDS_COURT_HOUSE_OU_CODE, WEST_MIDLANDS_LJA_NATIONAL_COURT_CODE);
        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(OTHER_COURT_HOUSE_OU_CODE, OTHER_LJA_NATIONAL_COURT_CODE);

        assignmentHelper = new AssignmentHelper();

        tflPiaCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(now().minusDays(30));

        tflPleadedGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(now().minusDays(10));


        tflPleadedNotGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(now().minusDays(10));

        tflPendingWithdrawalCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(now().minusDays(5));

        tvlPiaCasePayloadBuilder =
                CreateCase.CreateCasePayloadBuilder.withDefaults()
                        .withPostingDate(now().minusDays(30))
                        .withProsecutingAuthority(ProsecutingAuthority.TVL);


        tvlPleadedGuiltyRequestHearingCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(now().minusDays(10))
                .withProsecutingAuthority(ProsecutingAuthority.TVL);


        dvlaPiaCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(now().minusDays(31))
                .withProsecutingAuthority(ProsecutingAuthority.DVLA);

        dvlaPleadedNotGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(now().minusDays(5))
                .withProsecutingAuthority(ProsecutingAuthority.DVLA);

        databaseCleaner.cleanAll();

        final List<CreateCase.CreateCasePayloadBuilder> caseHelpers = Arrays.asList(
                tflPiaCasePayloadBuilder,
                tflPleadedGuiltyCasePayloadBuilder,
                tflPleadedNotGuiltyCasePayloadBuilder,
                tflPendingWithdrawalCasePayloadBuilder,
                tvlPleadedGuiltyRequestHearingCasePayloadBuilder,
                tvlPiaCasePayloadBuilder,
                dvlaPleadedNotGuiltyCasePayloadBuilder,
                dvlaPiaCasePayloadBuilder);

        caseHelpers.forEach(helper -> {
            stubGetEmptyAssignmentsByDomainObjectId(helper.getId());
            stubGetCaseDecisionsWithNoDecision(helper.getId());
        });

        caseHelpers.forEach(CreateCase::createCaseForPayloadBuilder);

        new UpdatePleaHelper(tflPleadedGuiltyCasePayloadBuilder.getId(), tflPleadedGuiltyCasePayloadBuilder.getOffenceId()).updatePlea(pleaPayload(GUILTY));
        new UpdatePleaHelper(tflPleadedNotGuiltyCasePayloadBuilder.getId(), tflPleadedNotGuiltyCasePayloadBuilder.getOffenceId()).updatePlea(pleaPayload(NOT_GUILTY));
        new UpdatePleaHelper(tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId(), tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getOffenceId()).updatePlea(pleaPayload(GUILTY_REQUEST_HEARING));
        new UpdatePleaHelper(dvlaPleadedNotGuiltyCasePayloadBuilder.getId(), dvlaPleadedNotGuiltyCasePayloadBuilder.getOffenceId()).updatePlea(pleaPayload(NOT_GUILTY));

        final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(tflPendingWithdrawalCasePayloadBuilder.getId(), SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);

        offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(UUID.randomUUID());
        offencesWithdrawalRequestHelper.verifyAllOffencesWithdrawalRequestedInPublicActiveMQ();
    }

    @After
    public void close() throws Exception {
        assignmentHelper.close();
    }

    @Test
    public void shouldAssignCasesAccordingToSessionTypeAndLocationAndPriorities() {
        verifyCaseAssignedFromMagistrateSession(OTHER_COURT_HOUSE_OU_CODE, dvlaPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(OTHER_COURT_HOUSE_OU_CODE);
        verifyCaseAssignedFromMagistrateSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE, tvlPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE);
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT_HOUSE_OU_CODE, tflPleadedGuiltyCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT_HOUSE_OU_CODE, tflPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(LONDON_COURT_HOUSE_OU_CODE);

        verifyCaseAssignedFromDelegatedPowersSession(OTHER_COURT_HOUSE_OU_CODE, dvlaPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseNotFoundInDelegatedPowersSession(OTHER_COURT_HOUSE_OU_CODE);
        verifyCaseAssignedFromDelegatedPowersSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE, tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId());
        verifyCaseNotFoundInDelegatedPowersSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE);
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE, tflPendingWithdrawalCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE, tflPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseNotFoundInDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE);
    }

    @Test
    public void londonCourtCanHandleNonTvlCases() {
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT_HOUSE_OU_CODE, tflPleadedGuiltyCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT_HOUSE_OU_CODE, dvlaPiaCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT_HOUSE_OU_CODE, tflPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(LONDON_COURT_HOUSE_OU_CODE);

        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE, tflPendingWithdrawalCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE, tflPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE, dvlaPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseNotFoundInDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE);
    }

    @Test
    public void westMidlandsCourtCanHandleNonTflCases() {
        verifyCaseAssignedFromMagistrateSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE, dvlaPiaCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE, tvlPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE);

        verifyCaseAssignedFromDelegatedPowersSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE, tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE, dvlaPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseNotFoundInDelegatedPowersSession(WEST_MIDLANDS_COURT_HOUSE_OU_CODE);
    }

    @Test
    public void shouldHandleConcurrentAssignmentRequestFromMultipleLegalAdvisers() {
        final Map<UUID, UUID> sessionIdByUserId = Stream.generate(UUID::randomUUID).limit(3).collect(toMap(identity(), la -> randomUUID()));

        sessionIdByUserId.entrySet().parallelStream().forEach(sessionByUser ->
                startSession(sessionByUser.getValue(), sessionByUser.getKey(), LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE));

        sessionIdByUserId.entrySet().parallelStream().forEach(sessionByUser ->
                assignmentHelper.requestCaseAssignment(sessionByUser.getValue(), sessionByUser.getKey(), ljaByCourtHouseOUCode.get(LONDON_COURT_HOUSE_OU_CODE), MAGISTRATE));

        final Map<UUID, UUID> assignedCaseByUserId = new HashMap<>(sessionIdByUserId.size());
        for (int i = 0; i < sessionIdByUserId.size(); i++) {
            final JsonObject assignment = assignmentHelper.getCaseAssignedEvent().payloadAsJsonObject();
            final UUID assigneeId = UUID.fromString(assignment.getString("assigneeId"));
            final UUID caseId = UUID.fromString(assignment.getString("caseId"));
            assignedCaseByUserId.put(assigneeId, caseId);
        }

        assertThat(assignedCaseByUserId.keySet(), containsInAnyOrder(sessionIdByUserId.keySet().toArray()));
        assertThat(assignedCaseByUserId.values(), containsInAnyOrder(
                tflPiaCasePayloadBuilder.getId(),
                tflPleadedGuiltyCasePayloadBuilder.getId(),
                dvlaPiaCasePayloadBuilder.getId())
        );
    }

    private void verifyCaseAssignedFromMagistrateSession(final String courtHouseOUCode, final UUID caseId) {
        verifyCaseAssigned(courtHouseOUCode, MAGISTRATE, caseId);
    }

    private void verifyCaseAssignedFromDelegatedPowersSession(final String courtHouseOUCode, final UUID caseId) {
        verifyCaseAssigned(courtHouseOUCode, DELEGATED_POWERS, caseId);
    }

    private void verifyCaseNotFoundInMagistrateSession(final String courtHouseOUCode) {
        verifyCaseNotFound(courtHouseOUCode, MAGISTRATE);
    }

    private void verifyCaseNotFoundInDelegatedPowersSession(final String courtHouseOUCode) {
        verifyCaseNotFound(courtHouseOUCode, DELEGATED_POWERS);
    }

    private void verifyCaseAssigned(final String courtHouseOUCode, final SessionType sessionType, final UUID caseId) {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        startSession(sessionId, userId, courtHouseOUCode, sessionType);
        assignmentHelper.requestCaseAssignment(sessionId, userId, ljaByCourtHouseOUCode.get(courtHouseOUCode), sessionType);

        final JsonEnvelope assignedEvent = assignmentHelper.getCaseAssignedEvent();

        assertThat(assignedEvent,
                jsonEnvelope(
                        metadata().withName("sjp.events.case-assigned"),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.assigneeId", equalTo(userId.toString())),
                                withJsonPath("$.caseAssignmentType", equalTo(sessionType == MAGISTRATE ? MAGISTRATE_DECISION.toString() : DELEGATED_POWERS_DECISION.toString()))
                        ))));
    }

    private void verifyCaseNotFound(final String courtHouseOUCode, final SessionType sessionType) {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        startSession(sessionId, userId, courtHouseOUCode, sessionType);
        assignmentHelper.requestCaseAssignment(sessionId, userId, ljaByCourtHouseOUCode.get(courtHouseOUCode), sessionType);

        final JsonEnvelope assignedEvent = assignmentHelper.getCaseNotAssignedEvent();

        assertThat(assignedEvent, not(nullValue()));
    }

    private static JsonObject pleaPayload(final String plea) {
        JsonObjectBuilder pleaBuilder = createObjectBuilder().add("plea", plea);
        if (!GUILTY.equals(plea)) {
            pleaBuilder.add("interpreterRequired", false);
        }
        return pleaBuilder.build();
    }

}
