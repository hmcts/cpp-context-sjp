package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.HttpClientUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PleadOnlineHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleadOnlineHelper.class);

    private final String writeUrl;

    public PleadOnlineHelper(final UUID caseId) {
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        writeUrl = String.format("/cases/%s/defendants/%s/plead-online", caseId, defendantId);
    }

    private void pleadOnline(final String payload, final String contentType) {
        LOGGER.info("Request payload: {}", new JsonPath(payload).prettify());
        HttpClientUtil.makePostCall(writeUrl, contentType, payload);
    }

    public void pleadOnline(final String payload) {
        pleadOnline(payload, "application/vnd.sjp.plead-online+json");
    }

    public static Response getOnlinePlea(final String caseId, final UUID userId) {
        final String resource = format("/cases/%s/defendants-online-plea", caseId);
        final String contentType = "application/vnd.sjp.query.defendants-online-plea+json";
        return HttpClientUtil.makeGetCall(resource, contentType, userId);
    }

    public static String getOnlinePlea(final String caseId, final Matcher<Object> jsonMatcher, final UUID userId) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> {
            Response onlinePlea = getOnlinePlea(caseId, userId);
            if (onlinePlea.getStatus() != OK.getStatusCode()) {
                fail("Polling interrupted, please fix the error before continue. Status code: " + onlinePlea.getStatus());
            }

            return onlinePlea.readEntity(String.class);
        }, jsonMatcher);
    }

    public static void verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(final UUID caseId, final boolean onlinePleaReceived) {
        pollWithDefaults(getCaseById(caseId))
                .timeout(20, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.onlinePleaReceived", is(onlinePleaReceived))
                        )
                );

    }
}
