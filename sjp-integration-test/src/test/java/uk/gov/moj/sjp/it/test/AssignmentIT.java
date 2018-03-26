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
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
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

    private static final String GUILTY = "GUILTY", NOT_GUILTY = "NOT_GUILTY", GUILTY_REQUEST_HEARING = "GUILTY_REQUEST_HEARING";
    private static final String LONDON_COURT = "2572", WEST_MIDLANDS_COURT = "2905", OTHER_COURT = "3000";
    private static final String MAGISTRATE = "Alan Smith";

    private CreateCase.CreateCasePayloadBuilder tflPiaCasePayloadBuilder, tflPleadedGuiltyCasePayloadBuilder, tflPleadedNotGuiltyCasePayloadBuilder, tflPendingWithdrawalCasePayloadBuilder,
            tvlPiaCasePayloadBuilder, tvlPleadedGuiltyRequestHearingCasePayloadBuilder, dvlaPiaCasePayloadBuilder, dvlaPleadedNotGuiltyCasePayloadBuilder;

    private SessionHelper sessionHelper;

    @Before
    public void init() throws Exception {
        sessionHelper = new SessionHelper();

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
        
        
        dvlaPiaCasePayloadBuilder =CreateCase.CreateCasePayloadBuilder.withDefaults()
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
        sessionHelper.close();
    }

    @Test
    public void shouldAssignCasesAccordingToSessionTypeAndLocationAndPriorities() {
        verifyCaseAssignedFromMagistrateSession(OTHER_COURT, dvlaPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(OTHER_COURT);
        verifyCaseAssignedFromMagistrateSession(WEST_MIDLANDS_COURT, tvlPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(WEST_MIDLANDS_COURT);
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, tflPleadedGuiltyCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, tflPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(LONDON_COURT);

        verifyCaseAssignedFromDelegatedPowersSession(OTHER_COURT, dvlaPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseNotFoundInDelegatedPowersSession(OTHER_COURT);
        verifyCaseAssignedFromDelegatedPowersSession(WEST_MIDLANDS_COURT, tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId());
        verifyCaseNotFoundInDelegatedPowersSession(WEST_MIDLANDS_COURT);
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, tflPendingWithdrawalCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, tflPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseNotFoundInDelegatedPowersSession(LONDON_COURT);
    }

    @Test
    public void londonCourtCanHandleNonTvlCases() {
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, tflPleadedGuiltyCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, dvlaPiaCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(LONDON_COURT, tflPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(LONDON_COURT);

        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, tflPendingWithdrawalCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, tflPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(LONDON_COURT, dvlaPleadedNotGuiltyCasePayloadBuilder.getId());
        verifyCaseNotFoundInDelegatedPowersSession(LONDON_COURT);
    }

    @Test
    public void westMidlandsCourtCanHandleNonTflCases() {
        verifyCaseAssignedFromMagistrateSession(WEST_MIDLANDS_COURT, dvlaPiaCasePayloadBuilder.getId());
        verifyCaseAssignedFromMagistrateSession(WEST_MIDLANDS_COURT, tvlPiaCasePayloadBuilder.getId());
        verifyCaseNotFoundInMagistrateSession(WEST_MIDLANDS_COURT);

        verifyCaseAssignedFromDelegatedPowersSession(WEST_MIDLANDS_COURT, tvlPleadedGuiltyRequestHearingCasePayloadBuilder.getId());
        verifyCaseAssignedFromDelegatedPowersSession(WEST_MIDLANDS_COURT, dvlaPleadedNotGuiltyCasePayloadBuilder.getId());
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
                tflPiaCasePayloadBuilder.getId().toString(),
                tflPleadedGuiltyCasePayloadBuilder.getId().toString(),
                dvlaPiaCasePayloadBuilder.getId().toString())
        );
    }

    private void verifyCaseAssignedFromMagistrateSession(final String courtCode, final UUID caseId) {
        verifyCaseAssigned(courtCode, Optional.of(MAGISTRATE), caseId);
    }

    private void verifyCaseAssignedFromDelegatedPowersSession(final String courtCode, final UUID caseId) {
        verifyCaseAssigned(courtCode, Optional.empty(), caseId);
    }

    private void verifyCaseNotFoundInMagistrateSession(final String courtCode) {
        verifyCaseNotFound(courtCode, Optional.of(MAGISTRATE));
    }

    private void verifyCaseNotFoundInDelegatedPowersSession(final String courtCode) {
        verifyCaseNotFound(courtCode, Optional.empty());
    }

    private void verifyCaseAssigned(final String courtCode, final Optional<String> magistrate, final UUID caseId) {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        sessionHelper.startSession(sessionId, userId, courtCode, magistrate);

        final JsonEnvelope assignedEvent = sessionHelper.getEventFromPrivateTopic();

        assertThat(assignedEvent,
                jsonEnvelope(
                        metadata().withName("sjp.events.case-assigned"),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
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
