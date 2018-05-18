package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.CASE_ASSIGNED_PRIVATE_EVENT;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.CASE_UNASSIGNED_EVENT;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseUnassignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.MAGISTRATE_SESSION_STARTED_EVENT;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startMagistrateSession;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.EventedListener;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class CaseUnassignmentIT extends BaseIntegrationTest {

    private static final UUID USER_ID = randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private static final String COURT_HOUSE_OU_CODE = "B01OK";


    private final AssignmentHelper assignmentHelper = new AssignmentHelper();
    private final SjpDatabaseCleaner cleaner = new SjpDatabaseCleaner();

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() throws SQLException {
        SchedulingStub.stubStartSjpSessionCommand();
        SchedulingStub.stubEndSjpSessionCommand();
        AssignmentStub.stubAddAssignmentCommand();
        AssignmentStub.stubRemoveAssignmentCommand();
        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(COURT_HOUSE_OU_CODE, "2572");

        cleaner.cleanAll();

        final String magistrate = "John Smith";

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        UUID caseId = createCasePayloadBuilder.getId();

        final JsonObject sessionStartedEvent = new EventedListener()
                .subscribe(MAGISTRATE_SESSION_STARTED_EVENT)
                .run(() -> startMagistrateSession(SESSION_ID, USER_ID, COURT_HOUSE_OU_CODE, magistrate))
                .popEvent(MAGISTRATE_SESSION_STARTED_EVENT)
                .get().payloadAsJsonObject();

        UUID sessionId = UUID.fromString(sessionStartedEvent.getString("sessionId"));
        assertThat(sessionId, equalTo(SESSION_ID));

        final JsonObject caseAssignedPrivateEvent = new EventedListener()
                .subscribe(CASE_ASSIGNED_PRIVATE_EVENT)
                .run(() -> requestCaseAssignment(SESSION_ID, USER_ID))
                .popEvent(CASE_ASSIGNED_PRIVATE_EVENT)
                .get().payloadAsJsonObject();

        final UUID caseIdActual = UUID.fromString(caseAssignedPrivateEvent.getString("caseId"));
        assertThat(caseId, equalTo(caseIdActual));
    }

    @Test
    public void unassignCase() {
        final JsonObject unassignment = new EventedListener()
                .subscribe(CASE_UNASSIGNED_EVENT)
                .run(() -> requestCaseUnassignment(createCasePayloadBuilder.getId(), USER_ID))
                .popEvent(CASE_UNASSIGNED_EVENT).get().payloadAsJsonObject();

        final UUID caseId = UUID.fromString(unassignment.getString("caseId"));
        assertThat(caseId, equalTo(createCasePayloadBuilder.getId()));

        assignmentHelper.assertCaseUnassigned(createCasePayloadBuilder.getId());
        AssignmentStub.verifyRemoveAssignmentCommandSend(caseId);
    }

}
