package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_MARKED_READY_FOR_DECISION;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.CASE_ASSIGNED_PRIVATE_EVENT;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.CASE_UNASSIGNED_EVENT;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.getCaseAssignment;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.isCaseAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseUnassignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.MAGISTRATE_SESSION_STARTED_EVENT;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startMagistrateSession;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
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

    private static final String COURT_HOUSE_OU_CODE = "B01OK";
    private final UUID userId = randomUUID();
    private final UUID sessionId = randomUUID();

    private final AssignmentHelper assignmentHelper = new AssignmentHelper();
    private final SjpDatabaseCleaner cleaner = new SjpDatabaseCleaner();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void assignCase() throws SQLException {
        SchedulingStub.stubStartSjpSessionCommand();
        SchedulingStub.stubEndSjpSessionCommand();
        AssignmentStub.stubAddAssignmentCommand();
        AssignmentStub.stubRemoveAssignmentCommand();
        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(COURT_HOUSE_OU_CODE, "2572");

        cleaner.cleanAll();

        final String magistrate = "John Smith";

        final UUID caseId = UUID.randomUUID();

        assertThat(getCaseAssignment(caseId, userId).getStatus(), is(NOT_FOUND.getStatusCode()));

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);

        final EventListener eventListener = new EventListener();

        eventListener
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));

        final JsonObject sessionStartedEvent = eventListener
                .reset()
                .subscribe(MAGISTRATE_SESSION_STARTED_EVENT)
                .run(() -> startMagistrateSession(sessionId, userId, COURT_HOUSE_OU_CODE, magistrate))
                .popEvent(MAGISTRATE_SESSION_STARTED_EVENT)
                .get().payloadAsJsonObject();

        assertThat(UUID.fromString(sessionStartedEvent.getString("sessionId")), equalTo(sessionId));

        final JsonObject caseAssignedPrivateEvent = new EventListener()
                .subscribe(CASE_ASSIGNED_PRIVATE_EVENT)
                .run(() -> requestCaseAssignment(sessionId, userId))
                .popEvent(CASE_ASSIGNED_PRIVATE_EVENT)
                .get().payloadAsJsonObject();

        final UUID caseIdActual = UUID.fromString(caseAssignedPrivateEvent.getString("caseId"));
        assertThat(caseId, equalTo(caseIdActual));
        assertThat(isCaseAssignedToUser(caseId, userId), is(true));
    }

    @Test
    public void unassignCase() {
        final JsonObject unassignment = new EventListener()
                .subscribe(CASE_UNASSIGNED_EVENT)
                .run(() -> requestCaseUnassignment(createCasePayloadBuilder.getId(), userId))
                .popEvent(CASE_UNASSIGNED_EVENT).get().payloadAsJsonObject();

        final UUID caseId = UUID.fromString(unassignment.getString("caseId"));
        assertThat(caseId, equalTo(createCasePayloadBuilder.getId()));

        AssignmentHelper.assertCaseUnassigned(createCasePayloadBuilder.getId());
        AssignmentStub.verifyRemoveAssignmentCommandSend(caseId);
        assertThat(isCaseAssignedToUser(caseId, userId), is(false));
    }

}
