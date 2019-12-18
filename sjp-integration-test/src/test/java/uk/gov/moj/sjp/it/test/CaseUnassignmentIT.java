package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.commandclient.AssignNextCaseClient;
import uk.gov.moj.sjp.it.commandclient.CreateCaseClient;
import uk.gov.moj.sjp.it.commandclient.StartSessionClient;
import uk.gov.moj.sjp.it.commandclient.UnassignCaseClient;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mortbay.log.Log;

public class CaseUnassignmentIT extends BaseIntegrationTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private static final String COURT_HOUSE_OU_CODE = "B01OK";
    private final UUID[] returnedSessionId = new UUID[1];
    private final String magistrate = "John Smith";

    private final SjpDatabaseCleaner cleaner = new SjpDatabaseCleaner();

    @Before
    public void setUp() throws SQLException {
        SchedulingStub.stubStartSjpSessionCommand();
        SchedulingStub.stubEndSjpSessionCommand();
        AssignmentStub.stubAssignmentReplicationCommands();
        ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery(COURT_HOUSE_OU_CODE, "2572");
        cleaner.cleanAll();

        CreateCaseClient createCase = CreateCaseClient.builder().id(CASE_ID).build();
        createCase.caseReceivedHandler = envelope -> Log.info("Case is created");
        Optional<Response> createCaseResponse = createCase.getExecutor().executeSync();
        assertThat(createCaseResponse.get().getStatus(), equalTo(202));

        StartSessionClient startSession = new StartSessionClient();
        startSession.sessionId = SESSION_ID;
        startSession.magistrate = magistrate;
        startSession.courtHouseOUCode = COURT_HOUSE_OU_CODE;
        startSession.magistrateStartedHandler = envelope -> {
            returnedSessionId[0] = UUID.fromString(((JsonEnvelope) envelope).payloadAsJsonObject().getString("sessionId"));
            assertThat(returnedSessionId[0], equalTo(SESSION_ID));
        };
        Optional<Response> sessionStartResponse = startSession.getExecutor().setExecutingUserId(USER_ID).executeSync();
        assertThat(sessionStartResponse.get().getStatus(), equalTo(202));

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().build();
        assignCase.sessionId = returnedSessionId[0];
        assignCase.assignedPrivateHandler = envelope -> {
            final UUID caseIdActual = UUID.fromString(((JsonEnvelope) envelope).payloadAsJsonObject().getString("caseId"));
            assertThat(CASE_ID, equalTo(caseIdActual));
        };
        Optional<Response> assignCaseResponse = assignCase.getExecutor().setExecutingUserId(USER_ID).executeSync();
        assertThat(assignCaseResponse.get().getStatus(), equalTo(202));
    }

    @Test
    @SuppressWarnings("squid:S1607")
    public void unassignCase() {
        UnassignCaseClient unassignCase = new UnassignCaseClient();
        unassignCase.caseId = CASE_ID;
        unassignCase.caseUnassignedHandler = envelope -> {
            final UUID caseId = UUID.fromString(((JsonEnvelope) envelope).payloadAsJsonObject().getString("caseId"));
            assertThat(caseId, equalTo(CASE_ID));

            AssignmentHelper.assertCaseUnassigned(CASE_ID);
            AssignmentStub.verifyRemoveAssignmentCommandSend(caseId);
        };

        Optional<Response> response = unassignCase.getExecutor().executeSync();
        assertThat(response.get().getStatus(), equalTo(202));
    }

}
