package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
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
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.DELEGATED_POWERS_DECISION;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENTS_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAsync;
import static uk.gov.moj.sjp.it.helper.UpdatePleaHelper.getPleaPayload;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.commandclient.AssignCaseClient;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestCancelHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.log.Log;

public class AssignmentRulesIT extends BaseIntegrationTest {

    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572", NON_LONDON_LJA_NATIONAL_COURT_CODE = "2905";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK00", NON_LONDON_COURT_HOUSE_OU_CODE = "B20EB00";
    private static final String DATE_TO_AVOID = "a-date-to-avoid";

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private CreateCase.CreateCasePayloadBuilder tflPiaCasePayloadBuilder, tflOldPiaCasePayloadBuilder, tflPleadedGuiltyCasePayloadBuilder, tflPleadedNotGuiltyCasePayloadBuilder, tflPendingWithdrawalCasePayloadBuilder,
            tvlPiaCasePayloadBuilder, tvlPleadedGuiltyRequestHearingCasePayloadBuilder, dvlaPiaCasePayloadBuilder, dvlaPleadedNotGuiltyCasePayloadBuilder;

    private UUID userId;

    @Before
    public void setUp() throws Exception {

        ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);
        ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery(NON_LONDON_COURT_HOUSE_OU_CODE, NON_LONDON_LJA_NATIONAL_COURT_CODE);
        AssignmentStub.stubAddAssignmentCommand();
        AssignmentStub.stubRemoveAssignmentCommand();
        SchedulingStub.stubStartSjpSessionCommand();

        final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();

        tflPiaCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(30));

        tflOldPiaCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(31));

        tflPleadedGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(10));

        tflPleadedNotGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(10));

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
                .withPostingDate(daysAgo(32))
                .withProsecutingAuthority(ProsecutingAuthority.DVLA);

        dvlaPleadedNotGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(5))
                .withProsecutingAuthority(ProsecutingAuthority.DVLA);

        databaseCleaner.cleanAll();

        final List<CreateCase.CreateCasePayloadBuilder> caseHelpers = Arrays.asList(
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
            stubGetCaseDecisionsWithNoDecision(helper.getId());
        });

        caseHelpers.forEach(CreateCase::createCaseForPayloadBuilder);

        updatePleaHelper.updatePlea(tflPleadedGuiltyCasePayloadBuilder.getId(), tflPleadedGuiltyCasePayloadBuilder.getOffenceId(), getPleaPayload(GUILTY));
        updatePleaHelper.updatePlea(tflPleadedNotGuiltyCasePayloadBuilder.getId(), tflPleadedNotGuiltyCasePayloadBuilder.getOffenceId(), getPleaPayload(NOT_GUILTY));
        updatePleaHelper.updatePlea(tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId(), tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getOffenceId(), getPleaPayload(GUILTY_REQUEST_HEARING));
        updatePleaHelper.updatePlea(dvlaPleadedNotGuiltyCasePayloadBuilder.getId(), dvlaPleadedNotGuiltyCasePayloadBuilder.getOffenceId(), getPleaPayload(NOT_GUILTY));

        final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(tflPendingWithdrawalCasePayloadBuilder.getId(), SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);

        userId = UUID.randomUUID();
        offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(userId);
        offencesWithdrawalRequestHelper.verifyAllOffencesWithdrawalRequestedInPublicActiveMQ();

        addDatesToAvoid(tflPleadedNotGuiltyCasePayloadBuilder.getId(), DATE_TO_AVOID);
        addDatesToAvoid(dvlaPleadedNotGuiltyCasePayloadBuilder.getId(), DATE_TO_AVOID);
    }

    @After
    public void cancelOffenceWithdrawalsAndCloseHelpers() {
        final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(tflPendingWithdrawalCasePayloadBuilder.getId(), SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED);
        offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(userId);
        offencesWithdrawalRequestCancelHelper.close();
    }

    @Test
    public void londonCourtsShouldHandleOnlyTflCases() {
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT_HOUSE_OU_CODE, tflPleadedGuiltyCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT_HOUSE_OU_CODE, tflOldPiaCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT_HOUSE_OU_CODE, tflPiaCasePayloadBuilder.getId());

        verifyCaseNotFoundInMagistrateSession(LONDON_COURT_HOUSE_OU_CODE);

        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE, tflPendingWithdrawalCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE, tflPleadedNotGuiltyCasePayloadBuilder.getId());

        verifyCaseNotFoundInDelegatedPowersSession(LONDON_COURT_HOUSE_OU_CODE);
    }

    @Test
    public void nonLondonCourtsShouldHandleOnlyNonTflCases() throws Exception {
        verifyCaseAssignedFromMagistrateSession(NON_LONDON_COURT_HOUSE_OU_CODE, dvlaPiaCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(NON_LONDON_COURT_HOUSE_OU_CODE, tvlPiaCasePayloadBuilder.getId());

        verifyCaseNotFoundInMagistrateSession(NON_LONDON_COURT_HOUSE_OU_CODE);

        verifyCaseAssignedFromDelegatedPowersSession(NON_LONDON_COURT_HOUSE_OU_CODE, tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(NON_LONDON_COURT_HOUSE_OU_CODE, dvlaPleadedNotGuiltyCasePayloadBuilder.getId());

        verifyCaseNotFoundInDelegatedPowersSession(NON_LONDON_COURT_HOUSE_OU_CODE);
    }

    @Test
    public void shouldHandleConcurrentAssignmentRequestFromMultipleLegalAdvisers() {
        final Map<UUID, UUID> sessionIdByUserId = Stream.generate(UUID::randomUUID).limit(3).collect(toMap(identity(), la -> randomUUID()));

        sessionIdByUserId
                .entrySet()
                .parallelStream()
                .forEach(sessionByUser -> startSessionAsync(sessionByUser.getValue(), sessionByUser.getKey(), LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE));

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

        AssignCaseClient assignCase = AssignCaseClient.builder().sessionId(sessionId).build();
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

        AssignCaseClient assignCase = AssignCaseClient.builder().sessionId(sessionId).build();
        assignCase.notAssignedHandler = (envelope) -> Log.info("Case Not Assigned");
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();
    }

    private static LocalDate daysAgo(int days) {
        return LocalDate.now().minusDays(days);
    }
}