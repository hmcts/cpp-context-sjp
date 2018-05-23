package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.CASE_ASSIGNED_PRIVATE_EVENT;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.assertCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.SessionHelper.DELEGATED_POWERS_SESSION_STARTED_EVENT;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAddAssignmentCommand;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubRemoveAssignmentCommand;
import static uk.gov.moj.sjp.it.stub.ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.pollUntilProcessDeleted;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.pollUntilProcessExists;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.EventedListener;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class AssignmentTimeoutIT extends BaseIntegrationTest {

    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";
    private static final String CASE_ASSIGNMENT_TIMEOUT_PROCESS_NAME = "sjpCaseAssignmentTimeout";

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();

    private UUID caseId;

    @Before
    public void setUp() throws Exception {
        databaseCleaner.cleanAll();
        caseId = UUID.randomUUID();

        stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);
        stubGetEmptyAssignmentsByDomainObjectId(caseId);
        stubGetCaseDecisionsWithNoDecision(caseId);
        stubAddAssignmentCommand();
        stubRemoveAssignmentCommand();
        stubStartSjpSessionCommand();

        createCaseAndWaitUntilReady(caseId);
    }

    @Test
    public void shouldCancelAssignmentTimerWhenDecisionSaved() {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        startSession(sessionId, userId);

        requestCaseAssignment(sessionId, userId);

        pollUntilProcessExists(CASE_ASSIGNMENT_TIMEOUT_PROCESS_NAME, caseId.toString());

        saveDecision(caseId);

        pollUntilProcessDeleted(CASE_ASSIGNMENT_TIMEOUT_PROCESS_NAME, caseId.toString(), "Timeout cancelled");

        assertCaseUnassigned(caseId);
    }

    private static void createCaseAndWaitUntilReady(final UUID caseId) {
        try (final MessageConsumerClient messageConsumerClient = new MessageConsumerClient()) {
            messageConsumerClient.startConsumer(CaseMarkedReadyForDecision.EVENT_NAME, "sjp.event");
            CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                    .withPostingDate(now().minusDays(30)).withId(caseId);
            CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
            messageConsumerClient.retrieveMessage();
        }
    }

    private static void startSession(final UUID sessionId, final UUID userId) {
        new EventedListener()
                .subscribe(DELEGATED_POWERS_SESSION_STARTED_EVENT)
                .run(() -> SessionHelper.startMagistrateSession(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE, "John Smith"));
    }

    private static void requestCaseAssignment(final UUID sessionId, final UUID userId) {
        new EventedListener()
                .subscribe(CASE_ASSIGNED_PRIVATE_EVENT)
                .run(() -> AssignmentHelper.requestCaseAssignment(sessionId, userId));
    }

    private static void saveDecision(final UUID caseId) {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        completeCaseProducer.completeCase();
        completeCaseProducer.assertCaseCompleted();
    }

}