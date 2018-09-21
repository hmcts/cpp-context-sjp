package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAddAssignmentCommand;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubRemoveAssignmentCommand;
import static uk.gov.moj.sjp.it.stub.ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.commandclient.AssignCaseClient;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class AssignmentReplicationIT extends BaseIntegrationTest {

    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";

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

        createCaseAndWaitUntilReady(caseId);
    }

    @Test
    public void shouldReplicateAssignmentEventsInAssignmentContext() {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        startSession(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);

        AssignCaseClient.builder().sessionId(sessionId).build().getExecutor().setExecutingUserId(userId).execute();

        AssignmentStub.verifyAddAssignmentCommandSent(caseId, userId, MAGISTRATE_DECISION);

        saveDecision(caseId);
        AssignmentStub.verifyRemoveAssignmentCommandSend(caseId);
    }

    private static void createCaseAndWaitUntilReady(final UUID caseId) {
        try (final MessageConsumerClient messageConsumerClient = new MessageConsumerClient()) {
            CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                    .withPostingDate(now().minusDays(30)).withId(caseId);
            CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
            messageConsumerClient.startConsumer(CaseMarkedReadyForDecision.EVENT_NAME, "sjp.event");
            messageConsumerClient.retrieveMessage();
        }
    }

    private static void saveDecision(final UUID caseId) {
        new CompleteCaseProducer(caseId).completeCase();
    }

}