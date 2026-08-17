package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class CaseHelper {

    private static final String QUERY_READY_CASES_RESOURCE = "/cases/ready-cases";
    private static final String QUERY_CASES_WITHOUT_DEFENDANT_POSTCODE_RESOURCE = "/cases/without-defendant-postcode";
    private static final String QUERY_READY_CASES = "application/vnd.sjp.query.ready-cases+json";
    private static final String QUERY_CASES_WITHOUT_DEFENDANT_POSTCODE = "application/vnd.sjp.query.cases-without-defendant-postcode+json";
    private static final String COMMAND_ADD_FINANCIAL_IMPOSITION_CORRELATION_ID = "application/vnd.sjp.add-financial-imposition-correlation-id+json";

    public static JsonObject pollUntilCaseReady(UUID caseId) {
        final JsonObject readyCases = pollReadyCasesUntilResponseIsJson(withJsonPath("readyCases.*", hasItem(
                isJson(
                        withJsonPath("caseId", Matchers.equalTo(caseId.toString())))
        )));

        return readyCases.getJsonArray("readyCases").getValuesAs(JsonObject.class).stream()
                .filter(readyCase -> readyCase.getString("caseId").equals(caseId.toString()))
                .findFirst()
                .get();
    }

    public static JsonObject pollUntilCaseNotReady(UUID caseId) {
        return pollReadyCasesUntilResponseIsJson(withJsonPath("readyCases.*", not(hasItem(
                isJson(withJsonPath("caseId", Matchers.equalTo(caseId.toString())))
        ))));
    }

    public static JsonObject pollReadyCasesUntilResponseIsJson(final Matcher<? super ReadContext> matcher) {
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(QUERY_READY_CASES_RESOURCE), QUERY_READY_CASES)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
        return pollWithDefaultsUntilResponseIsJson(requestParams.build(), matcher);
    }

    public static JsonObject pollCasesWithoutDefendantPostcodeUntileResponseIsJson(final Matcher<? super ReadContext> matcher) {
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(QUERY_CASES_WITHOUT_DEFENDANT_POSTCODE_RESOURCE), QUERY_CASES_WITHOUT_DEFENDANT_POSTCODE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
        return pollWithDefaultsUntilResponseIsJson(requestParams.build(), matcher);
    }

    public static void addFinancialImpositionCorrelationId(final UUID caseId, final UUID defendantId, final UUID correlationId) {
        final String url = format("/cases/%s/defendant/%s/add-financial-imposition-correlation-id", caseId.toString(), defendantId.toString());
        final JsonObject payload = createObjectBuilder()
                .add("correlationId", correlationId.toString())
                .build();
        makePostCall(USER_ID, url, COMMAND_ADD_FINANCIAL_IMPOSITION_CORRELATION_ID, payload.toString(), ACCEPTED);
    }
}
