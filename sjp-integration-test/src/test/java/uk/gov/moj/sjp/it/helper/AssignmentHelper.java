package uk.gov.moj.sjp.it.helper;

import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.HttpClientUtil;

import javax.json.Json;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.is;

public class AssignmentHelper {

    public static final String CASE_ASSIGNMENT_REJECTED_PRIVATE_EVENT = CaseAssignmentRejected.EVENT_NAME;
    public static final String CASE_ASSIGNMENT_REJECTED_PUBLIC_EVENT = "public.sjp.case-assignment-rejected";
    public static final String CASE_ASSIGNED_PUBLIC_EVENT = "public.sjp.case-assigned";
    public static final String CASE_ASSIGNED_PRIVATE_EVENT = CaseAssigned.EVENT_NAME;
    public static final String CASE_NOT_ASSIGNED_EVENT = "public.sjp.case-not-assigned";
    public static final String CASE_UNASSIGNED_EVENT = CaseUnassigned.EVENT_NAME;

    public static UUID requestCaseAssignment(final UUID sessionId, final UUID userId) {
        final String contentType = "application/vnd.sjp.assign-case+json";
        final String url = String.format("/sessions/%s", sessionId);
        return HttpClientUtil.makePostCall(userId, url, contentType, Json.createObjectBuilder().build().toString(), ACCEPTED);
    }

    public static void assertCaseAssigned(final UUID caseId) {
        CasePoller.pollUntilCaseByIdIsOk(caseId, withJsonPath("$.unassigned", is(true)));
    }

    public static UUID requestCaseUnassignment(final UUID caseId, final UUID userId) {
        final String contentType = "application/vnd.sjp.unassign-case+json";
        final String url = String.format("/cases/%s/unassign", caseId);
        return HttpClientUtil.makePostCall(userId, url, contentType, Json.createObjectBuilder().build().toString(), ACCEPTED);
    }

    public static void assertCaseUnassigned(final UUID caseId) {
        CasePoller.pollUntilCaseByIdIsOk(caseId, withJsonPath("$.assigned", is(false)));
    }


}
