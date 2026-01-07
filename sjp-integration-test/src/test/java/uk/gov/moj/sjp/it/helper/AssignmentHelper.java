package uk.gov.moj.sjp.it.helper;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.POLL_INTERVAL;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.TIMEOUT_IN_SECONDS;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;


public class AssignmentHelper {

    public static UUID requestCaseAssignmentAsync(final UUID sessionId, final UUID userId) {
        final String contentType = "application/vnd.sjp.assign-next-case+json";
        final String url = String.format("/sessions/%s", sessionId);
        return makePostCall(userId, url, contentType, "{}", ACCEPTED);
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
        return makeGetCall(url, contentType, userId);
    }

    public static void requestCaseAssignmentAndConfirm(final UUID sessionId, final UUID userId, final UUID caseId) {
        requestCaseAssignmentAsync(sessionId, userId);
        assignCaseToUser(caseId, userId, UUID.randomUUID(), ACCEPTED);
        pollUntilCaseAssignedToUser(caseId, userId);
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

    public static boolean pollUntilCaseAssignedToUser(final UUID caseId, final UUID userId) {
        return await().pollInterval(POLL_INTERVAL).atMost(TIMEOUT_IN_SECONDS, SECONDS).until(() -> isCaseAssignedToUser(caseId, userId), is(true));
    }

    public static boolean pollUntilCaseNotAssignedToUser(final UUID caseId, final UUID userId) {
        return await().pollInterval(POLL_INTERVAL).atMost(TIMEOUT_IN_SECONDS, SECONDS).until(() -> isCaseAssignedToUser(caseId, userId), is(false));
    }

    public static void pollCaseUnassigned(final UUID caseId) {
        pollUntilCaseByIdIsOk(caseId, withJsonPath("$.assigned", is(false)));
    }

    public static UUID assignCaseToUser(final UUID caseId,
                                        final UUID assigneeId,
                                        final UUID callerId,
                                        final Response.Status expectedStatus) {
        final String contentType = "application/vnd.sjp.assign-case+json";
        final String url = String.format("/cases/%s", caseId);

        final JsonObject payload = createObjectBuilder()
                .add("userId", assigneeId.toString())
                .build();

        return makePostCall(callerId, url, contentType, payload.toString(), expectedStatus);
    }
}
