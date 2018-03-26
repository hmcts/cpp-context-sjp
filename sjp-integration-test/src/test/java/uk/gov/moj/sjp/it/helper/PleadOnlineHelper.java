package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;

import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.ResponseData;

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

    public PleadOnlineHelper(UUID caseId) {
        
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        writeUrl = String.format("/cases/%s/defendants/%s/plead-online", caseId, defendantId);
    }

    private void pleadOnline(final String payload,
                            final String contentType) {
        LOGGER.info("Request payload: {}", new JsonPath(payload).prettify());
        HttpClientUtil.makePostCall(writeUrl, contentType, payload);
    }

    public void pleadOnline(final String payload) {
        pleadOnline(payload, "application/vnd.sjp.plead-online+json");
    }

    public Response getOnlinePlea(final String caseId) {
        final String resource = format("/cases/%s/defendants-online-plea", caseId);
        final String contentType = "application/vnd.sjp.query.defendants-online-plea+json";
        return HttpClientUtil.makeGetCall(resource, contentType);
    }

    public String getOnlinePlea(final String caseId, final Matcher jsonMatcher) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> getOnlinePlea(caseId).readEntity(String.class), jsonMatcher);
    }

    public void verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(String caseId, boolean onlinePleaReceived) {
        final ResponseData caseResponse = poll(getCaseById(caseId))
                .timeout(20, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.onlinePleaReceived", is(onlinePleaReceived))
                        )
                );

    }
}
