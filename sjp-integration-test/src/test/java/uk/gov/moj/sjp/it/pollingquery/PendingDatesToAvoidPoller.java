package uk.gov.moj.sjp.it.pollingquery;

import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;

public class PendingDatesToAvoidPoller {

    private static RequestParamsBuilder getDatesToAvoid(final String userId, final String prosecutingAuthority) {
        final String url = prosecutingAuthority != null ? "/cases/pending-dates-to-avoid?prosecutingAuthority="
                + prosecutingAuthority : "/cases/pending-dates-to-avoid";

        final String contentType = "application/vnd.sjp.query.pending-dates-to-avoid+json";
        return requestParams(getReadUrl(url), contentType)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static JsonPath pollUntilPendingDatesToAvoidIsOk(final String userId, final Matcher<? super ReadContext> jsonPayloadMatcher,
                                                            final String prosecutingAuthority) {
        return new JsonPath(pollWithDefaults(getDatesToAvoid(userId, prosecutingAuthority))
                .until(
                        status().is(OK),
                        payload().isJson(jsonPayloadMatcher)
                ).getPayload());
    }
}
