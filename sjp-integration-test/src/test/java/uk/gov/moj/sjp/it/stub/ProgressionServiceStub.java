package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.sjp.it.util.JsonHelper.lenientCompare;
import static uk.gov.moj.sjp.it.util.JsonHelper.strictCompareIgnoringSelectedData;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.POLL_INTERVAL;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.TIMEOUT_IN_SECONDS;

import uk.gov.justice.services.test.utils.core.http.FibonacciPollWithStartAndMax;
import uk.gov.moj.sjp.it.util.JsonHelper;

import java.time.Duration;
import java.util.List;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class ProgressionServiceStub {

    public static final String REFER_TO_COURT_COMMAND_URL = "/progression-service/command/api/rest/progression/refertocourt";
    public static final String REFER_TO_COURT_COMMAND_CONTENT = "application/vnd.progression.refer-cases-to-court+json";

    public static void stubReferCaseToCourtCommand() {
        stubFor(post(urlPathEqualTo(REFER_TO_COURT_COMMAND_URL))
                .withHeader(CONTENT_TYPE, equalTo(REFER_TO_COURT_COMMAND_CONTENT))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)));
    }

    public static void verifyReferToCourtCommandSent(final JsonObject expectedCommandPayload) {
        await().pollInterval(POLL_INTERVAL)
                .atMost(TIMEOUT_IN_SECONDS, SECONDS).until(() ->
                        findAll(postRequestedFor(urlPathMatching(REFER_TO_COURT_COMMAND_URL + ".*"))
                                .withHeader(CONTENT_TYPE, WireMock.equalTo(REFER_TO_COURT_COMMAND_CONTENT)))
                                .stream()
                                .map(LoggedRequest::getBodyAsString)
                                .map(JsonHelper::getJsonObject)
                                .anyMatch(commandPayload -> lenientCompare(commandPayload, expectedCommandPayload)));
    }

    public static void verifyReferToCourtCommandSentStrictMode(final JsonObject expectedCommandPayload, final List<String> ignoreCompareList) {
        await().pollInterval(POLL_INTERVAL)
                .atMost(TIMEOUT_IN_SECONDS, SECONDS)
                .until(() ->
                        findAll(postRequestedFor(urlPathMatching(REFER_TO_COURT_COMMAND_URL + ".*"))
                                .withHeader(CONTENT_TYPE, WireMock.equalTo(REFER_TO_COURT_COMMAND_CONTENT)))
                                .stream()
                                .map(LoggedRequest::getBodyAsString)
                                .map(JsonHelper::getJsonObject)
                                .anyMatch(commandPayload -> strictCompareIgnoringSelectedData(commandPayload, expectedCommandPayload, ignoreCompareList)));
    }
}
