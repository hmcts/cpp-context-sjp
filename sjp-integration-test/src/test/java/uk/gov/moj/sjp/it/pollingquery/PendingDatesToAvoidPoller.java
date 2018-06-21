package uk.gov.moj.sjp.it.pollingquery;

import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;

public class PendingDatesToAvoidPoller {
    private static final int POLLING_TIMEOUT = 20;

    private static RequestParamsBuilder getDatesToAvoid(final String userId) {
        final String contentType = "application/vnd.sjp.query.pending-dates-to-avoid+json";
        return requestParams(getReadUrl("/cases/pending-dates-to-avoid"), contentType)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static JsonPath pollUntilPendingDatesToAvoidIsOk(final String userId, final Matcher<? super ReadContext> jsonPayloadMatcher) {
        return new JsonPath(pollWithDefaults(getDatesToAvoid(userId))
                .timeout(POLLING_TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(jsonPayloadMatcher)
                ).getPayload());
    }
}
