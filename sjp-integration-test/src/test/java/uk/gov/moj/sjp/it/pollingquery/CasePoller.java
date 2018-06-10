package uk.gov.moj.sjp.it.pollingquery;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.services.test.utils.core.http.ResponseData;

import java.util.UUID;

import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;

public class CasePoller {

    private static final int POLLING_TIMEOUT = 40;

    public static JsonPath pollUntilCaseByIdIsOk(final UUID caseId) {
        return pollUntilCaseByIdIsOk(caseId, any(ReadContext.class));
    }

    public static JsonPath pollUntilCaseByIdIsOk(final UUID caseId, final Matcher<? super ReadContext> jsonPayloadMatcher) {
        ResponseData responseData = pollWithDefaults(getCaseById(caseId))
                .timeout(POLLING_TIMEOUT, SECONDS)
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

}
