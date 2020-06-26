package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.sjp.it.Constants.CASE_ADJOURNED_TO_LATER_SJP_EVENT;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SET_PLEAS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.createCase;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.requestSetPleas;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.verifyCaseStatus;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.verifyEventEmittedForSetPleas;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.verifyUpdatedOffencesWithPleas;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Enable this when merging to master")
public class SetPleasIT extends BaseIntegrationTest {

    private final UUID sessionId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID offence1Id = randomUUID();
    private final UUID offence2Id = randomUUID();
    private final UUID offence3Id = randomUUID();
    private UUID defendantId;

    private static final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

    private final EventListener eventListener = new EventListener();
    private static final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();

    private Map<UUID, PleaType> pleaTypesByOffence = new HashMap<>();
    private CreateCase.CreateCasePayloadBuilder aCase;

    @Before
    public void setUp() throws Exception {
        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubDefaultCourtByCourtHouseOUCodeQuery();

        databaseCleaner.cleanViewStore();

        final CreateCase.DefendantBuilder defendantBuilder = CreateCase.DefendantBuilder.withDefaults();
        defendantId = defendantBuilder.getId();
        stubEnforcementAreaByPostcode(defendantBuilder.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        aCase = createCase(caseId, defendantBuilder, offence1Id, offence2Id, offence3Id, postingDate);
        final ProsecutingAuthority prosecutingAuthority = aCase.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        startSession(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);

        requestCaseAssignment(sessionId, USER_ID);

        pleaTypesByOffence.put(offence1Id, GUILTY);
        pleaTypesByOffence.put(offence2Id, NOT_GUILTY);
        pleaTypesByOffence.put(offence3Id, GUILTY_REQUEST_HEARING);
    }

    @Test
    public void shouldVerifySetPleas() {
        requestSetPleas(
                caseId,
                eventListener,
                true,
                false,
                true,
                null,
                false,
                asList(Triple.of(offence1Id, defendantId, GUILTY),
                        Triple.of(offence2Id, defendantId, NOT_GUILTY),
                        Triple.of(offence3Id, defendantId, GUILTY_REQUEST_HEARING)),
                PleasSet.EVENT_NAME, PleadedNotGuilty.EVENT_NAME, PleadedGuilty.EVENT_NAME, PleadedGuiltyCourtHearingRequested.EVENT_NAME, PUBLIC_EVENT_SET_PLEAS);

        verifyEventEmittedForSetPleas(eventListener,
                caseId,
                defendantId,
                false,
                null,
                false,
                pleaTypesByOffence,
                PleasSet.EVENT_NAME, PleadedNotGuilty.EVENT_NAME, PleadedGuilty.EVENT_NAME, PleadedGuiltyCourtHearingRequested.EVENT_NAME, PUBLIC_EVENT_SET_PLEAS);

        verifyUpdatedOffencesWithPleas(caseId, pleaTypesByOffence);

        // verify case readiness state
        verifyCaseStatus(caseId, PLEA_RECEIVED_READY_FOR_DECISION);
    }

    @Test
    public void shouldVerifyHearingLanguagePreferenceCancelled() {
        requestSetPleas(
                caseId, eventListener,
                true,
                true,
                true,
                null,
                false,
                asList(Triple.of(offence1Id, defendantId, GUILTY),
                        Triple.of(offence2Id, defendantId, NOT_GUILTY),
                        Triple.of(offence3Id, defendantId, GUILTY_REQUEST_HEARING)),
                PleasSet.EVENT_NAME, HearingLanguagePreferenceUpdatedForDefendant.EVENT_NAME);

        verifyEventEmittedForSetPleas(eventListener,
                caseId,
                defendantId,
                true,
                null,
                false,
                pleaTypesByOffence,
                PleasSet.EVENT_NAME, HearingLanguagePreferenceUpdatedForDefendant.EVENT_NAME);

        requestSetPleas(
                caseId,
                eventListener,
                false,
                false,
                false,
                null,
                false,
                asList(Triple.of(offence1Id, defendantId, GUILTY),
                        Triple.of(offence2Id, defendantId, GUILTY),
                        Triple.of(offence3Id, defendantId, GUILTY)),
                PleasSet.EVENT_NAME, HearingLanguagePreferenceCancelledForDefendant.EVENT_NAME);

        pleaTypesByOffence.put(offence1Id, GUILTY);
        pleaTypesByOffence.put(offence2Id, GUILTY);
        pleaTypesByOffence.put(offence3Id, GUILTY);

        verifyEventEmittedForSetPleas(eventListener,
                caseId,
                defendantId,
                null,
                null,
                null,
                pleaTypesByOffence,
                PleasSet.EVENT_NAME, HearingLanguagePreferenceCancelledForDefendant.EVENT_NAME);
        verifyUpdatedOffencesWithPleas(caseId, pleaTypesByOffence);

        // verify case readiness state
        verifyCaseStatus(caseId, PLEA_RECEIVED_READY_FOR_DECISION);
    }

    @Test
    public void shouldVerifyPleaCancelled() {
        requestSetPleas(caseId,
                eventListener,
                true,
                true,
                true,
                null,
                false,
                asList(Triple.of(offence1Id, defendantId, GUILTY),
                        Triple.of(offence2Id, defendantId, NOT_GUILTY),
                        Triple.of(offence3Id, defendantId, GUILTY_REQUEST_HEARING)),
                PleasSet.EVENT_NAME);

        verifyEventEmittedForSetPleas(eventListener,
                caseId,
                defendantId,
                true,
                null,
                false,
                pleaTypesByOffence,
                PleasSet.EVENT_NAME);

        requestSetPleas(
                caseId,
                eventListener.reset(),
                false,
                false,
                false,
                null,
                false,
                asList(Triple.of(offence1Id, defendantId, null),
                        Triple.of(offence2Id, defendantId, GUILTY),
                        Triple.of(offence3Id, defendantId, GUILTY)),
                PleasSet.EVENT_NAME, PleaCancelled.EVENT_NAME);

        pleaTypesByOffence.put(offence1Id, null);
        pleaTypesByOffence.put(offence2Id, GUILTY);
        pleaTypesByOffence.put(offence3Id, GUILTY);

        verifyEventEmittedForSetPleas(eventListener,
                caseId,
                defendantId,
                null,
                null,
                null,
                pleaTypesByOffence,
                PleaCancelled.EVENT_NAME);

        verifyUpdatedOffencesWithPleas(caseId, pleaTypesByOffence);
    }

    @Test
    public void shouldVerifyInterpreterCancelled() {
        requestSetPleas(
                caseId,
                eventListener,
                true,
                false,
                true,
                "GERMAN",
                true,
                asList(Triple.of(offence1Id, defendantId, GUILTY),
                        Triple.of(offence2Id, defendantId, NOT_GUILTY),
                        Triple.of(offence3Id, defendantId, GUILTY_REQUEST_HEARING)),
                PleasSet.EVENT_NAME, InterpreterUpdatedForDefendant.EVENT_NAME);

        verifyEventEmittedForSetPleas(eventListener,
                caseId,
                defendantId,
                false,
                "GERMAN",
                true,
                pleaTypesByOffence,
                PleasSet.EVENT_NAME, InterpreterUpdatedForDefendant.EVENT_NAME);

        requestSetPleas(
                caseId,
                eventListener.reset(),
                true,
                false,
                true,
                null,
                false,
                asList(Triple.of(offence1Id, defendantId, GUILTY),
                        Triple.of(offence2Id, defendantId, NOT_GUILTY),
                        Triple.of(offence3Id, defendantId, GUILTY_REQUEST_HEARING)),
                PleasSet.EVENT_NAME, InterpreterCancelledForDefendant.EVENT_NAME);
        verifyEventEmittedForSetPleas(eventListener,
                caseId,
                defendantId,
                false,
                null,
                false,
                pleaTypesByOffence,
                PleasSet.EVENT_NAME, InterpreterCancelledForDefendant.EVENT_NAME);

        verifyUpdatedOffencesWithPleas(caseId, pleaTypesByOffence);
    }

    /**
     * Legal advisor marks takes a final decision on one offence(eg: dismiss) and adjourns another
     * decision. <br/> A plea request from client should be rejected on an offence with final
     * decision
     */
    @Test
    public void shouldRejectAPleaOnFinalDecision() {

        //Given
        pleaTypesByOffence.clear();
        final LocalDate adjournTo = now().plusDays(10);
        final User user = new User("John", "Smith", USER_ID);

        final Adjourn adjournDecision = new Adjourn(null, singletonList(createOffenceDecisionInformation(offence1Id, NO_VERDICT)), "More info needed", adjournTo);

        final Dismiss dismiss1 = DismissBuilder.withDefaults(offence2Id).build();
        final Dismiss dismiss2 = DismissBuilder.withDefaults(offence3Id).build();
        final List<? extends OffenceDecision> offenceDecisions = asList(adjournDecision, dismiss1, dismiss2);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, null);
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), Integer.toString((new Random()).nextInt(4)), "Any Court");

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUpdateRejected.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        //When
        requestSetPleas(
                caseId,
                eventListener,
                true,
                false,
                true,
                null,
                false,
                singletonList(Triple.of(offence2Id, defendantId, GUILTY)),
                PleasSet.EVENT_NAME, InterpreterUpdatedForDefendant.EVENT_NAME);

        //Then
        final CaseUpdateRejected caseUpdateRejected = eventListener.popEventPayload(CaseUpdateRejected.class);
        assertThat(caseUpdateRejected.getReason(), is(CaseUpdateRejected.RejectReason.PLEA_REJECTED_AS_FINAL_DECISION_TAKEN_FOR_OFFENCE));
    }

    @Test
    public void shouldRejectSetPleaOnPostConvictionAdjournment() {

        //Given
        pleaTypesByOffence.clear();
        final LocalDate adjournTo = now().plusDays(10);
        final User user = new User("John", "Smith", USER_ID);

        final Adjourn adjournDecision = new Adjourn(null, singletonList(createOffenceDecisionInformation(offence1Id, PROVED_SJP)), "More info needed", adjournTo);
        final Dismiss dismiss1 = DismissBuilder.withDefaults(offence2Id).build();
        final Dismiss dismiss2 = DismissBuilder.withDefaults(offence3Id).build();
        final List<? extends OffenceDecision> offenceDecisions = asList(adjournDecision, dismiss1, dismiss2);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, null);
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), Integer.toString((new Random()).nextInt(4)), "Any Court");

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUpdateRejected.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        //When
        requestSetPleas(
                caseId,
                eventListener,
                true,
                false,
                true,
                null,
                false,
                singletonList(Triple.of(offence1Id, defendantId, GUILTY)),
                PleasSet.EVENT_NAME, InterpreterUpdatedForDefendant.EVENT_NAME);

        //Then
        final CaseUpdateRejected caseUpdateRejected = eventListener.popEventPayload(CaseUpdateRejected.class);
        assertThat(caseUpdateRejected.getReason(), is(CaseUpdateRejected.RejectReason.OFFENCE_HAS_CONVICTION));
    }

    @Test
    public void shouldAcceptAPleaOnAdjournDecision() {

        //Given
        pleaTypesByOffence.clear();
        final LocalDate adjournTo = now().plusDays(10);
        final User user = new User("John", "Smith", USER_ID);

        final Adjourn adjournDecision = new Adjourn(null, singletonList(createOffenceDecisionInformation(offence1Id, NO_VERDICT)), "More info needed", adjournTo);
        final Dismiss dismiss1 = DismissBuilder.withDefaults(offence2Id).build();
        final Dismiss dismiss2 = DismissBuilder.withDefaults(offence3Id).build();
        final List<? extends OffenceDecision> offenceDecisions = asList(adjournDecision, dismiss1, dismiss2);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offenceDecisions, null);
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), Integer.toString((new Random()).nextInt(4)), "Any Court");

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CASE_ADJOURNED_TO_LATER_SJP_EVENT)
                .run(() -> DecisionHelper.saveDecision(decision));

        //When
        requestSetPleas(
                caseId,
                eventListener,
                true,
                false,
                true,
                null,
                false,
                singletonList(Triple.of(offence1Id, defendantId, GUILTY)),
                PleasSet.EVENT_NAME, PleadedGuilty.EVENT_NAME, CaseStatusChanged.EVENT_NAME);

        //Then
        Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(CASE_ADJOURNED_TO_LATER_SJP_EVENT);
        final PleadedGuilty pleadedGuilty = eventListener.popEventPayload(PleadedGuilty.class);
        final CaseStatusChanged caseStatusChanged = eventListener.popEventPayload(CaseStatusChanged.class);

        assertThat(jsonEnvelope.isPresent(), CoreMatchers.is(true));
        assertThat(pleadedGuilty.getOffenceId(), equalTo(offence1Id));
        assertThat(caseStatusChanged.getCaseId(), equalTo(caseId));

    }
}
