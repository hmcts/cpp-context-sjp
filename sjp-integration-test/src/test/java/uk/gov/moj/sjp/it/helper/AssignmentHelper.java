package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.HttpClientUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;


public class AssignmentHelper {

    public static UUID requestCaseAssignmentAsync(final UUID sessionId, final UUID userId) {
        final String contentType = "application/vnd.sjp.assign-case+json";
        final String url = String.format("/sessions/%s", sessionId);
        return HttpClientUtil.makePostCall(userId, url, contentType, "{}", ACCEPTED);
    }

    public static Optional<JsonEnvelope> requestCaseAssignmentAndWaitForEvent(final UUID sessionId, final UUID userId, final String expectedEvent) {
        return new EventListener()
                .subscribe(expectedEvent)
                .run(() -> requestCaseAssignmentAsync(sessionId, userId))
                .popEvent(expectedEvent);
    }

    public static Response getCaseAssignment(final UUID caseId, final UUID userId) {
        final String contentType = "application/vnd.sjp.query.case-assignment+json";
        final String url = String.format("/cases/%s/assignment", caseId);
        return HttpClientUtil.makeGetCall(url, contentType, userId);
    }

    public static Optional<JsonEnvelope> requestCaseAssignment(final UUID sessionId, final UUID userId) {
        return requestCaseAssignmentAndWaitForEvent(sessionId, userId, CaseAssigned.EVENT_NAME);
    }

    public static boolean isCaseAssignedToUser(final UUID caseId, final UUID userId) {
        return Stream.of(getCaseAssignment(caseId, userId))
                .filter(response -> response.getStatus() == OK.getStatusCode())
                .map(response -> new JsonPath(response.readEntity(String.class)).getBoolean("assignedToMe"))
                .findFirst()
                .orElse(false);
    }

    public static void assertCaseUnassigned(final UUID caseId) {
        CasePoller.pollUntilCaseByIdIsOk(caseId, withJsonPath("$.assigned", is(false)));
    }

}
