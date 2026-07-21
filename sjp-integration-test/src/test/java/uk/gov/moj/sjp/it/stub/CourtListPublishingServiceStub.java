package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.POLL_INTERVAL;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.TIMEOUT_IN_SECONDS;

import java.util.Collection;
import java.util.List;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtListPublishingServiceStub {

    private static final String PUBLISH_COURT_LIST_URL = "/api/court-list-publish/sjp/publishCourtList";
    private final static Logger LOGGER = LoggerFactory.getLogger(CourtListPublishingServiceStub.class);

    public static void stubPublishCourtListEndpoint() {
        stubFor(post(urlPathEqualTo(PUBLISH_COURT_LIST_URL))
                .willReturn(aResponse().withStatus(SC_OK)));
    }

    public static List<JSONObject> pollCourtListPublishRequests(final Matcher<Collection<?>> matcher) {
        try {
            return await().pollInterval(POLL_INTERVAL)
                    .atMost(TIMEOUT_IN_SECONDS, SECONDS).until(() ->
                    findAll(postRequestedFor(urlPathEqualTo(PUBLISH_COURT_LIST_URL)))
                            .stream()
                            .map(LoggedRequest::getBodyAsString)
                            .map(JSONObject::new)
                            .collect(toList()), matcher);
        } catch (final ConditionTimeoutException timeoutException) {
            LOGGER.info("Exception while finding the captured requests in wire mock:" + timeoutException);
            return emptyList();
        }
    }
}
