package uk.gov.moj.sjp.it.helper;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.sjp.it.util.JsonHelper;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class CaseAccountHelper {

    public static JsonObject pollForCaseAccountNote(final String caseUrn, final Matcher<? super ReadContext> matcher, final UUID userId) {
        final String url = String.format("/cases/%s/account-note", caseUrn);
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(url), "application/vnd.sjp.query.account-note+json")
                .withHeader(HeaderConstants.USER_ID, userId);
        return pollWithDefaultsUntilResponseIsJson(requestParams.build(), matcher);
    }
}
