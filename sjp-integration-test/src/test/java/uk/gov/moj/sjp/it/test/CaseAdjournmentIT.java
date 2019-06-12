package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseNotReady;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.helper.UpdatePleaHelper.getPleaPayload;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffenceById;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestCancelHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.ReadyCaseHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.producer.CaseAdjournmentProducer;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.ActivitiHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class CaseAdjournmentIT extends BaseIntegrationTest {

    private UUID caseId;
    private UUID sessionId;
    private UUID userId;
    private LocalDate adjournmentDate = now().plusDays(7);
    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";
    private static final String PROCESS_NAME = "caseState";
    final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

    private UUID defendantId;
    private UUID offenceId;

    @Before
    public void setUp() throws SQLException {
        caseId = randomUUID();
        sessionId = randomUUID();
        userId = randomUUID();
        offenceId = randomUUID();

        databaseCleaner.cleanAll();

        AssignmentStub.stubAddAssignmentCommand();
        AssignmentStub.stubRemoveAssignmentCommand();
        SchedulingStub.stubStartSjpSessionCommand();
        stubQueryOffenceById(randomUUID());
        ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(postingDate);
        defendantId = createCasePayloadBuilder.getDefendantBuilder().getId();
        offenceId = createCasePayloadBuilder.getOffenceBuilder().getId();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);

        startSession(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
    }

    @Test
    public void shouldRecordCaseAdjournmentAndChangeCaseStatusToNotReady() {
        requestCaseAssignment(sessionId, userId);
        pollUntilCaseByIdIsOk(caseId, caseAssigned(true));

        caseAdjournedRecordedPrivateEventCreated();

        pollUntilCaseNotReady(caseId);
        pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));
    }

    @Test
    public void shouldRemoveAdjournToDateWhenWithdrawalReceivedAndCaseCompleted() {
        requestCaseAssignment(sessionId, userId);
        pollUntilCaseByIdIsOk(caseId, caseAssigned(true));

        caseAdjournedRecordedPrivateEventCreated();

        try (OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId)) {
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(USER_ID);
            pollUntilCaseReady(caseId);
            pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));
        }

        new CompleteCaseProducer(caseId, defendantId, offenceId).completeCase();

        pollUntilCaseByIdIsOk(caseId, CoreMatchers.allOf(
                withJsonPath("$.completed", CoreMatchers.is(true)),
                withJsonPath("$.status", CoreMatchers.is(CaseStatus.COMPLETED.name())),
                withoutJsonPath("$.adjournedTo")
        ));
    }

    @Test
    public void shouldChangeCaseReadinessStateWhenAdjournmentDateElapsed() throws Exception {
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId);
             final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseId);
             final ReadyCaseHelper readyCaseHelper = new ReadyCaseHelper()
        ) {

            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseId,
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.GUILTY));
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY);
            pollUntilCaseReady(caseId);

            caseAdjournedRecordedPrivateEventCreated();
            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, adjournmentDate);
            pollUntilCaseNotReady(caseId);

            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(USER_ID);
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, WITHDRAWAL_REQUESTED);
            pollUntilCaseReady(caseId);

            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(USER_ID);
            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, adjournmentDate);
            pollUntilCaseNotReady(caseId);

            final String pendingAdjournmentProcess = ActivitiHelper.pollUntilProcessExists(PROCESS_NAME, caseId.toString());
            ActivitiHelper.executeTimerJobs(pendingAdjournmentProcess);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY);
            pollUntilCaseReady(caseId);
        }
    }

    @Test
    public void shouldNotChangeCaseReadinessStateForAdjournedCaseWhen28DaysElapsed() throws Exception {
        final LocalDate datesToAvoidDeadline = now().plusDays(NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID);
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(caseId, offenceId);
             final ReadyCaseHelper readyCaseHelper = new ReadyCaseHelper()
        ) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseId,
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.GUILTY));
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY);
            pollUntilCaseReady(caseId);

            caseAdjournedRecordedPrivateEventCreated();
            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, adjournmentDate);
            pollUntilCaseNotReady(caseId);

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.NOT_GUILTY));
            readyCaseHelper.verifyCaseExpectedDateReadyChangedEventEmitted(caseId, adjournmentDate, datesToAvoidDeadline);
            pollUntilCaseNotReady(caseId);
            pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.GUILTY));
            readyCaseHelper.verifyCaseExpectedDateReadyChangedEventEmitted(caseId, datesToAvoidDeadline, adjournmentDate);
            pollUntilCaseNotReady(caseId);
            pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));

            cancelPleaHelper.cancelPlea();
            pollUntilCaseNotReady(caseId);
            pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));
        }
    }

    private void caseAdjournedRecordedPrivateEventCreated() {
        final CaseAdjournmentProducer caseAdjournmentProducer = new CaseAdjournmentProducer(caseId, sessionId, adjournmentDate);

        final EventListener eventListener = new EventListener();
        final Optional<JsonEnvelope> caseAdjournmentRecordedEvent = eventListener
                .subscribe("sjp.events.case-adjourned-to-later-sjp-hearing-recorded")
                .run(caseAdjournmentProducer::adjournCase)
                .popEvent("sjp.events.case-adjourned-to-later-sjp-hearing-recorded");

        assertTrue(caseAdjournmentRecordedEvent.isPresent());

        final JsonObject event = caseAdjournmentRecordedEvent.get().payloadAsJsonObject();
        assertThat(event.getString("adjournedTo"), is(adjournmentDate.toString()));
        assertThat(event.getString("caseId"), is(caseId.toString()));
        assertThat(event.getString("sessionId"), is(sessionId.toString()));
    }

    private Matcher caseAssigned(final boolean assigned) {
        return withJsonPath("$.assigned", is(assigned));
    }

    private Matcher caseAdjourned(final LocalDate adjournedTo) {
        return withJsonPath("$.adjournedTo", is(adjournedTo.toString()));
    }
}
