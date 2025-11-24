package uk.gov.moj.sjp.it.pollingquery;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED_APPLICATION_PENDING;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getPotentialCasesByDefendantId;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getProsecutionCaseById;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.sjp.it.util.JsonHelper;

import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;

public class CasePoller {

    public static JsonPath pollUntilCaseByIdIsOk(final UUID caseId) {
        return pollUntilCaseByIdIsOk(caseId, any(ReadContext.class));
    }

    public static JsonPath pollUntilCaseHasStatus(final UUID caseId, final CaseStatus caseStatus) {
        final Matcher statusMatcher = withJsonPath("$.status", is(caseStatus.name()));
        return pollUntilCaseByIdIsOk(caseId, statusMatcher);
    }

    public static JsonPath pollUntilCaseByIdIsOk(final UUID caseId, final Matcher<? super ReadContext> jsonPayloadMatcher) {
        ResponseData responseData = pollWithDefaults(getCaseById(caseId)).logging()
                .until(
                        anyOf(
                                allOf(
                                        status().is(OK),
                                        payload().isJson(jsonPayloadMatcher)),
                                status().is(INTERNAL_SERVER_ERROR),
                                status().is(FORBIDDEN))
                );

        if (responseData.getStatus() != OK) {
            fail("Polling interrupted, please fix the error before continue. Status code: " + responseData.getStatus());
        }

        return new JsonPath(responseData.getPayload());
    }

    public static JsonPath pollUntilProsecutionCaseByIdIsOk(final UUID caseId, final Matcher<? super ReadContext> jsonPayloadMatcher) {
        ResponseData responseData = pollWithDefaults(getProsecutionCaseById(caseId)).logging()
                .until(
                        anyOf(
                                allOf(
                                        status().is(OK),
                                        payload().isJson(jsonPayloadMatcher)),
                                status().is(INTERNAL_SERVER_ERROR),
                                status().is(FORBIDDEN))
                );

        if (responseData.getStatus() != OK) {
            fail("Polling interrupted, please fix the error before continue. Status code: " + responseData.getStatus());
        }

        return new JsonPath(responseData.getPayload());
    }

    public static String pollUntilPotentialCasesByDefendantIdIsOk(final UUID defendantId) {
        ResponseData responseData = pollWithDefaults(getPotentialCasesByDefendantId(defendantId)).logging()
                .until(
                        anyOf(
                                allOf(
                                        status().is(OK),
                                        payload().isJson(any(ReadContext.class))),
                                status().is(INTERNAL_SERVER_ERROR),
                                status().is(FORBIDDEN))
                );

        if (responseData.getStatus() != OK) {
            fail("Polling interrupted, please fix the error before continue. Status code: " + responseData.getStatus());
        }

        return responseData.getPayload();
    }

    public static JsonObject getCase(final UUID caseId, final Matcher<? super ReadContext> jsonPayloadMatcher) {
        return JsonHelper.getJsonObject(pollUntilCaseByIdIsOk(caseId, jsonPayloadMatcher).prettyPrint());
    }

    public static void pollForCase(final UUID caseId, final Matcher[] matchers) {
        pollUntilCaseByIdIsOk(caseId, allOf(matchers));
    }

    public static void pollUntilCaseStatusCompleted(final UUID caseId) {
        pollUntilCaseHasStatus(caseId, COMPLETED);
    }

}
