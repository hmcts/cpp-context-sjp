package uk.gov.moj.sjp.it.pollingquery;

import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;

public class CasePoller {
    private static final int POLLING_TIMEOUT = 40;

    public static JsonPath pollUntilCaseByIdIsOk(final UUID caseId) {
        return pollUntilCaseByIdIsOk(caseId, CoreMatchers.any(ReadContext.class));
    }

    public static JsonPath pollUntilCaseByIdIsOk(final UUID caseId, final Matcher<? super ReadContext> jsonPayloadMatcher) {
        return new JsonPath(poll(getCaseById(caseId.toString()))
                .timeout(POLLING_TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(
                                jsonPayloadMatcher
                        )
                ).getPayload());
    }


}
