package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.DELEGATED_POWERS_DECISION;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.EventSelector.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AssignmentIT extends BaseIntegrationTest {

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();

    private static final String TFL = "TFL", TVL = "TVL", DVLA = "DVLA";
    private static final String GUILTY = "GUILTY", NOT_GUILTY = "NOT_GUILTY", GUILTY_REQUEST_HEARING = "GUILTY_REQUEST_HEARING";
    private static final String LONDON_COURT = "2572", WEST_MIDLANDS_COURT = "2905", OTHER_COURT = "3000";
    private static final String MAGISTRATE = "Alan Smith";

    private CaseSjpHelper tflPiaCaseHelper, tflPleadedGuiltyCaseHelper, tflPleadedNotGuiltyCaseHelper, tflPendingWithdrawalCaseHelper,
            tvlPiaCaseHelper, tvlPleadedNotGuiltyCaseHelper, dvlaPiaCaseHelper, dvlaPleadedNotGuiltyCaseHelper;

    private SessionHelper sessionHelper;

    private List<CaseSjpHelper> caseHelpers;

    @Before
    public void init() throws Exception {
        sessionHelper = new SessionHelper();

        tflPiaCaseHelper = new CaseSjpHelper(now().minusDays(30), TFL);
        tflPleadedGuiltyCaseHelper = new CaseSjpHelper(now().minusDays(10), TFL);
        tflPleadedNotGuiltyCaseHelper = new CaseSjpHelper(now().minusDays(10), TFL);
        tflPendingWithdrawalCaseHelper = new CaseSjpHelper(now().minusDays(5), TFL);
        tvlPiaCaseHelper = new CaseSjpHelper(now().minusDays(30), TVL);
        tvlPleadedNotGuiltyCaseHelper = new CaseSjpHelper(now().minusDays(10), TVL);
        dvlaPiaCaseHelper = new CaseSjpHelper(now().minusDays(31), DVLA);
        dvlaPleadedNotGuiltyCaseHelper = new CaseSjpHelper(now().minusDays(5), DVLA);

        databaseCleaner.cleanAll();

        caseHelpers = Arrays.asList(
                tflPiaCaseHelper,
                tflPleadedGuiltyCaseHelper,
                tflPleadedNotGuiltyCaseHelper,
                tflPendingWithdrawalCaseHelper,
                tvlPleadedNotGuiltyCaseHelper,
                tvlPiaCaseHelper,
                dvlaPleadedNotGuiltyCaseHelper,
                dvlaPiaCaseHelper);

        caseHelpers.forEach(helper -> {
            stubGetEmptyAssignmentsByDomainObjectId(helper.getCaseId());
            stubGetCaseDecisionsWithNoDecision(helper.getCaseId());
        });

        caseHelpers.forEach(CaseSjpHelper::createCase);
        caseHelpers.forEach(CaseSjpHelper::verifyCaseCreatedUsingId);

        new UpdatePleaHelper(tflPleadedGuiltyCaseHelper).updatePlea(pleaPayload(GUILTY));
        new UpdatePleaHelper(tflPleadedNotGuiltyCaseHelper).updatePlea(pleaPayload(NOT_GUILTY));
        new UpdatePleaHelper(tvlPleadedNotGuiltyCaseHelper).updatePlea(pleaPayload(GUILTY_REQUEST_HEARING));
        new UpdatePleaHelper(dvlaPleadedNotGuiltyCaseHelper).updatePlea(pleaPayload(NOT_GUILTY));

        final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(tflPendingWithdrawalCaseHelper, SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);

        offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(UUID.randomUUID());
        offencesWithdrawalRequestHelper.verifyAllOffencesWithdrawalRequestedInPublicActiveMQ();
    }

    @After
    public void close() throws Exception {
        sessionHelper.close();
        caseHelpers.forEach(CaseSjpHelper::close);
    }

    @Test
    public void shouldAssignCasesAccordingToSessionTypeAndLocationAndPriorities() {
        verifyCaseAssignedFromMagistrateSession(OTHER_COURT, dvlaPiaCaseHelper.getCaseId());
        verifyCaseNotFoundInMagistrateSession(OTHER_COURT);
        verifyCaseAssignedFromMagistrateSession(WEST_MIDLANDS_COURT, tvlPiaCaseHelper.getCaseId());
        verifyCaseNotFoundInMagistrateSession(WEST_MIDLANDS_COURT);
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, tflPleadedGuiltyCaseHelper.getCaseId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, tflPiaCaseHelper.getCaseId());
        verifyCaseNotFoundInMagistrateSession(LONDON_COURT);

        verifyCaseAssignedFromDelegatedPowersSession(OTHER_COURT, dvlaPleadedNotGuiltyCaseHelper.getCaseId());
        verifyCaseNotFoundInDelegatedPowersSession(OTHER_COURT);
        verifyCaseAssignedFromDelegatedPowersSession(WEST_MIDLANDS_COURT, tvlPleadedNotGuiltyCaseHelper.getCaseId());
        verifyCaseNotFoundInDelegatedPowersSession(WEST_MIDLANDS_COURT);
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, tflPendingWithdrawalCaseHelper.getCaseId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, tflPleadedNotGuiltyCaseHelper.getCaseId());
        verifyCaseNotFoundInDelegatedPowersSession(LONDON_COURT);
    }

    @Test
    public void londonCourtCanHandleNonTvlCases() {
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, tflPleadedGuiltyCaseHelper.getCaseId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, dvlaPiaCaseHelper.getCaseId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, tflPiaCaseHelper.getCaseId());
        verifyCaseNotFoundInMagistrateSession(LONDON_COURT);

        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, tflPendingWithdrawalCaseHelper.getCaseId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, tflPleadedNotGuiltyCaseHelper.getCaseId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, dvlaPleadedNotGuiltyCaseHelper.getCaseId());
        verifyCaseNotFoundInDelegatedPowersSession(LONDON_COURT);
    }

    @Test
    public void westMidlandsCourtCanHandleNonTflCases() {
        verifyCaseAssignedFromMagistrateSession(WEST_MIDLANDS_COURT, dvlaPiaCaseHelper.getCaseId());
        verifyCaseAssignedFromMagistrateSession(WEST_MIDLANDS_COURT, tvlPiaCaseHelper.getCaseId());
        verifyCaseNotFoundInMagistrateSession(WEST_MIDLANDS_COURT);

        verifyCaseAssignedFromDelegatedPowersSession(WEST_MIDLANDS_COURT, tvlPleadedNotGuiltyCaseHelper.getCaseId());
        verifyCaseAssignedFromDelegatedPowersSession(WEST_MIDLANDS_COURT, dvlaPleadedNotGuiltyCaseHelper.getCaseId());
        verifyCaseNotFoundInDelegatedPowersSession(WEST_MIDLANDS_COURT);
    }

    @Test
    public void shouldHandleConcurrentAssignmentRequestFromMultipleLegalAdvisers() {
        final List<UUID> legalAdvisers = Arrays.asList(randomUUID(), randomUUID(), randomUUID());

        legalAdvisers.parallelStream().forEach(legalAdviser ->
                sessionHelper.startSession(randomUUID(), legalAdviser, LONDON_COURT, Optional.of(MAGISTRATE)));

        final Map<UUID, String> assignedCaseByLegalAdviser = legalAdvisers.stream()
                .map(legalAdviser -> sessionHelper.getEventFromPrivateTopic())
                .collect(toMap(
                        assignmentEvent -> UUID.fromString(assignmentEvent.metadata().userId().get()),
                        assignmentEvent -> assignmentEvent.payloadAsJsonObject().getString("caseId")
                ));

        assertThat(assignedCaseByLegalAdviser.keySet(), containsInAnyOrder(legalAdvisers.toArray()));
        assertThat(assignedCaseByLegalAdviser.values(), containsInAnyOrder(
                tflPiaCaseHelper.getCaseId(),
                tflPleadedGuiltyCaseHelper.getCaseId(),
                dvlaPiaCaseHelper.getCaseId())
        );
    }

    private void verifyCaseAssignedFromMagistrateSession(final String courtCode, final String caseId) {
        verifyCaseAssigned(courtCode, Optional.of(MAGISTRATE), caseId);
    }

    private void verifyCaseAssignedFromDelegatedPowersSession(final String courtCode, final String caseId) {
        verifyCaseAssigned(courtCode, Optional.empty(), caseId);
    }

    private void verifyCaseNotFoundInMagistrateSession(final String courtCode) {
        verifyCaseNotFound(courtCode, Optional.of(MAGISTRATE));
    }

    private void verifyCaseNotFoundInDelegatedPowersSession(final String courtCode) {
        verifyCaseNotFound(courtCode, Optional.empty());
    }

    private void verifyCaseAssigned(final String courtCode, final Optional<String> magistrate, final String caseId) {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        sessionHelper.startSession(sessionId, userId, courtCode, magistrate);

        final JsonEnvelope assignedEvent = sessionHelper.getEventFromPrivateTopic();

        assertThat(assignedEvent,
                jsonEnvelope(
                        metadata().withName("sjp.events.case-assigned"),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId)),
                                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                withJsonPath("$.assigneeId", equalTo(userId.toString())),
                                withJsonPath("$.caseAssignmentType", equalTo(magistrate.map(m -> MAGISTRATE_DECISION).orElse(DELEGATED_POWERS_DECISION).toString()))
                        ))));
    }

    private void verifyCaseNotFound(final String courtCode, final Optional<String> magistrate) {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        sessionHelper.startSession(sessionId, userId, courtCode, magistrate);

        final JsonEnvelope assignedEvent = sessionHelper.getEventFromPublicTopic();

        assertThat(assignedEvent,
                jsonEnvelope(
                        metadata().withName("public.sjp.session-started"),
                        payload().isJson(allOf(
                                withoutJsonPath("$.caseId"),
                                withJsonPath("$.sessionId", equalTo(sessionId.toString()))
                        ))));
    }

    private static JsonObject pleaPayload(final String plea) {
        JsonObjectBuilder pleaBuilder = createObjectBuilder().add("plea", plea);
        if (!GUILTY.equals(plea)) {
            pleaBuilder.add("interpreterRequired", false);
        }
        return pleaBuilder.build();
    }
}
