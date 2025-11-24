package uk.gov.moj.sjp.it.helper;

import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

import java.util.UUID;

import javax.json.JsonObject;

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
