package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;

import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class ReadyCaseHelper {

    private static final String QUERY_READY_CASES_RESOURCE = "/cases/ready-cases";
    private static final String QUERY_READY_CASES = "application/vnd.sjp.query.ready-cases+json";


    public static void pollUntilReadyWithReason(final UUID caseId, final CaseReadinessReason readinessReason) {
        pollReadyCasesUntilResponseIsJson(withJsonPath("readyCases.*", hasItem(
                isJson(allOf(
                        withJsonPath("caseId", equalTo(caseId.toString())),
                        withJsonPath("reason", equalTo(readinessReason.name())))
                ))));
    }

    private static JsonObject pollReadyCasesUntilResponseIsJson(final Matcher<? super ReadContext> matcher) {
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(QUERY_READY_CASES_RESOURCE), QUERY_READY_CASES)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
        return pollWithDefaultsUntilResponseIsJson(requestParams.build(), matcher);
    }
}
