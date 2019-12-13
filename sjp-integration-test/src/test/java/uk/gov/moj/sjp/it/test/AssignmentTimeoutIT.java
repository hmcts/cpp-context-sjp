package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.assertCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.pollUntilProcessDeleted;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.pollUntilProcessExists;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER_ID;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class AssignmentTimeoutIT extends BaseIntegrationTest {

    private static final String CASE_ASSIGNMENT_TIMEOUT_PROCESS_NAME = "sjpCaseAssignmentTimeout";

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();

    private static final UUID CASE_ID = randomUUID(), OFFENCE_ID = randomUUID();

    private static CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
            .withPostingDate(now().minusDays(30)).withId(CASE_ID).withOffenceId(OFFENCE_ID);
    ;

    @Before
    public void setUp() throws Exception {
        databaseCleaner.cleanAll();

        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubGetEmptyAssignmentsByDomainObjectId(CASE_ID);
        stubAssignmentReplicationCommands();
        stubStartSjpSessionCommand();

        createCaseAndWaitUntilReady(CASE_ID, OFFENCE_ID);
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
    }

    @Test
    public void shouldCancelAssignmentTimerWhenDecisionSaved() {
        final UUID sessionId = randomUUID();

        startSession(sessionId, DEFAULT_USER_ID);

        requestCaseAssignment(sessionId, DEFAULT_USER_ID);

        pollUntilProcessExists(CASE_ASSIGNMENT_TIMEOUT_PROCESS_NAME, CASE_ID.toString());

        DecisionHelper.saveDefaultDecisionInSession(CASE_ID, sessionId, DEFAULT_USER_ID, asList(OFFENCE_ID));

        pollUntilProcessDeleted(CASE_ASSIGNMENT_TIMEOUT_PROCESS_NAME, CASE_ID.toString(), "Timeout cancelled");

        assertCaseUnassigned(CASE_ID);
    }

    private static void createCaseAndWaitUntilReady(final UUID caseId, final UUID offenceId) {
        try (final MessageConsumerClient messageConsumerClient = new MessageConsumerClient()) {
            messageConsumerClient.startConsumer(CaseMarkedReadyForDecision.EVENT_NAME, "sjp.event");
            CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
            messageConsumerClient.retrieveMessage();
        }
    }

    private static void startSession(final UUID sessionId, final UUID userId) {
        SessionHelper.startMagistrateSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, "John Smith");
    }

}