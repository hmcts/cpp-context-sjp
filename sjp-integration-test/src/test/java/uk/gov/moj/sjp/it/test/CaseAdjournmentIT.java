package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.Priority.HIGH;
import static uk.gov.moj.cpp.sjp.domain.Priority.MEDIUM;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.CASE_ADJOURNED_TO_LATER_SJP_EVENT;
import static uk.gov.moj.sjp.it.Constants.EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SET_PLEAS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseNotReady;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.requestSetPleas;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.ReadyCaseHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;
import uk.gov.moj.sjp.it.util.ActivitiHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class CaseAdjournmentIT extends BaseIntegrationTest {

    private static final String TIMER_TIMEOUT_PROCESS_NAME = "timerTimeout";
    private final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final LocalDate adjournmentDate = now().plusDays(7);
    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private UUID caseId;
    private UUID sessionId;
    private UUID offenceId;
    private UUID withdrawalRequestReasonId = randomUUID();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private EventListener eventListener = new EventListener();
    private User user = new User("John", "Smith", randomUUID());

    @Before
    public void setUp() throws SQLException {
        caseId = randomUUID();
        sessionId = randomUUID();
        offenceId = randomUUID();
        databaseCleaner.cleanAll();

        AssignmentStub.stubAssignmentReplicationCommands();
        SchedulingStub.stubStartSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();

        ReferenceDataServiceStub.stubWithdrawalReasonsQuery(withdrawalRequestReasonId, "Insufficient Evidence");
        UsersGroupsStub.stubForUserDetails(user, "ALL");

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(postingDate);

        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");

        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);

        startSession(sessionId, user.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
    }

    @Test
    public void shouldRecordCaseAdjournmentAndChangeCaseStatusToNotReady() {
        adjournOffence();

        pollUntilCaseNotReady(caseId); // TODO should be handled as part of ATCM-4395
        pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));
    }

    @Test
    public void shouldRemoveAdjournToDateWhenWithdrawalReceivedAndCaseCompleted() {
        adjournOffence();

        final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(USER_ID);
        offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(caseId, offencesWithdrawalRequestHelper.preparePayloadWithDefaultsForCase(createCasePayloadBuilder));
        pollUntilCaseReady(caseId);
        pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));

        DecisionHelper.saveDefaultDecision(caseId, offenceId);

        pollUntilCaseByIdIsOk(caseId, CoreMatchers.allOf(
                withJsonPath("$.completed", CoreMatchers.is(true)),
                withJsonPath("$.status", CoreMatchers.is(CaseStatus.COMPLETED.name())),
                withoutJsonPath("$.adjournedTo")
        ));
    }

    @Test
    public void shouldChangeCaseReadinessStateWhenAdjournmentDateElapsed() throws Exception {
        try (final ReadyCaseHelper readyCaseHelper = new ReadyCaseHelper()) {

            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            requestSetPleas(caseId,
                    eventListener,
                    true,
                    false,
                    true,
                    null,
                    false,
                    asList(Triple.of(createCasePayloadBuilder.getOffenceId(),
                            createCasePayloadBuilder.getDefendantBuilder().getId(),
                            GUILTY)),
                    PUBLIC_EVENT_SET_PLEAS);
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY, MAGISTRATE, MEDIUM);
            pollUntilCaseReady(caseId);

            adjournOffence();

            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, adjournmentDate); // TODO should be handled as part of ATCM-4395
            pollUntilCaseNotReady(caseId);

            OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(user.getUserId(), EVENT_OFFENCES_WITHDRAWAL_STATUS_SET);
            offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(caseId, getRequestWithdrawalPayload(createCasePayloadBuilder.getOffenceId()));

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, WITHDRAWAL_REQUESTED, DELEGATED_POWERS, HIGH);
            pollUntilCaseReady(caseId);

            offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(caseId, new ArrayList<>());
            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, adjournmentDate);
            pollUntilCaseNotReady(caseId);

            final String pendingAdjournmentProcess = ActivitiHelper.pollUntilProcessExists(TIMER_TIMEOUT_PROCESS_NAME, caseId.toString());
            ActivitiHelper.executeTimerJobs(pendingAdjournmentProcess);

            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY, MAGISTRATE, MEDIUM);
            pollUntilCaseReady(caseId);
        }

    }

    @Test
    public void shouldNotChangeCaseReadinessStateForAdjournedCaseWhen28DaysElapsed() throws Exception {
        try (final ReadyCaseHelper readyCaseHelper = new ReadyCaseHelper()) {

            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            requestSetPleas(caseId,
                    eventListener,
                    true,
                    false,
                    true,
                    null,
                    false,
                    asList(Triple.of(createCasePayloadBuilder.getOffenceId(),
                            createCasePayloadBuilder.getDefendantBuilder().getId(),
                            GUILTY)),
                    PUBLIC_EVENT_SET_PLEAS);
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY, MAGISTRATE, MEDIUM);
            pollUntilCaseReady(caseId);

            adjournOffence();

            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, adjournmentDate);  // TODO should be handled as part of ATCM-4395
            pollUntilCaseNotReady(caseId);

            requestSetPleas(caseId,
                    eventListener,
                    true,
                    false,
                    true,
                    null,
                    false,
                    asList(Triple.of(createCasePayloadBuilder.getOffenceId(), createCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY)),
                    PUBLIC_EVENT_SET_PLEAS);

            // TODO: SEPARATE STORY FOR CaseExpectedDateReadyChanged
            //readyCaseHelper.verifyCaseExpectedDateReadyChangedEventEmitted(caseId, adjournmentDate, datesToAvoidDeadline);
            pollUntilCaseNotReady(caseId);
            pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));

            requestSetPleas(caseId,
                    eventListener,
                    true,
                    false,
                    true,
                    null,
                    false,
                    asList(Triple.of(createCasePayloadBuilder.getOffenceId(), createCasePayloadBuilder.getDefendantBuilder().getId(), GUILTY)),
                    PUBLIC_EVENT_SET_PLEAS);

            // TODO: SEPARATE STORY FOR CaseExpectedDateReadyChanged
            //readyCaseHelper.verifyCaseExpectedDateReadyChangedEventEmitted(caseId, datesToAvoidDeadline, adjournmentDate);
            pollUntilCaseNotReady(caseId);
            pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));

            requestSetPleas(caseId,
                    eventListener,
                    true,
                    false,
                    true,
                    null,
                    false,
                    asList(Triple.of(createCasePayloadBuilder.getOffenceId(), createCasePayloadBuilder.getDefendantBuilder().getId(), null)),
                    PUBLIC_EVENT_SET_PLEAS);
            pollUntilCaseNotReady(caseId);
            pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));
        }

    }

    private void adjournOffence() {
        requestCaseAssignment(sessionId, user.getUserId());

        final Adjourn adjournDecision = new Adjourn(null, asList(new OffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT)), "reason", adjournmentDate);
        final List<Adjourn> offencesDecisions = asList(adjournDecision);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        final Optional<JsonEnvelope> caseAdjournmentRecordedEvent = new EventListener()
                .subscribe(CASE_ADJOURNED_TO_LATER_SJP_EVENT)
                .run(() -> DecisionHelper.saveDecision(decision))
                .popEvent(CASE_ADJOURNED_TO_LATER_SJP_EVENT);

        assertTrue(caseAdjournmentRecordedEvent.isPresent());
    }

    private Matcher caseAssigned(final boolean assigned) {
        return withJsonPath("$.assigned", is(assigned));
    }

    private Matcher caseAdjourned(final LocalDate adjournedTo) {
        return withJsonPath("$.adjournedTo", is(adjournedTo.toString()));
    }

    private List<WithdrawalRequestsStatus> getRequestWithdrawalPayload(UUID offence1Id) {
        final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses = new ArrayList<>();
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offence1Id, withdrawalRequestReasonId));
        return withdrawalRequestsStatuses;
    }
}
