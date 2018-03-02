package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.String.format;

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
}
