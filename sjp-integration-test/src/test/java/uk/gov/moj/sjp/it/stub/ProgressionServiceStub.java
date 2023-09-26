package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.await;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.sjp.it.util.JsonHelper.lenientCompare;
import static uk.gov.moj.sjp.it.util.JsonHelper.strictCompareIgnoringSelectedData;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.sjp.it.util.JsonHelper;

import java.util.List;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class ProgressionServiceStub {

    public static final String REFER_TO_COURT_COMMAND_URL = "/progression-service/command/api/rest/progression/refertocourt";
    public static final String REFER_TO_COURT_COMMAND_CONTENT = "application/vnd.progression.refer-cases-to-court+json";

    private static final String CASE_QUERY_API_URL = "/progression-service/query/api/rest/prosecutioncases/%s";
    private static final String CASE_QUERY_CONTENT = "application/vnd.progression.query.case+json";

    public static void stubReferCaseToCourtCommand() {
        InternalEndpointMockUtils.stubPingFor("progression-service");

        stubFor(post(urlPathEqualTo(REFER_TO_COURT_COMMAND_URL))
                .withHeader(CONTENT_TYPE, equalTo(REFER_TO_COURT_COMMAND_CONTENT))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)));
    }

    public static void stubProgressionCaseQuery(String progressionCaseId, String payload) {
        InternalEndpointMockUtils.stubPingFor("progression-service");

        final String urlPath = format(CASE_QUERY_API_URL, progressionCaseId);
        stubFor(get(urlPathEqualTo(urlPath))
                 .withHeader(ACCEPT, equalTo(CASE_QUERY_CONTENT))
                 .willReturn(aResponse().withStatus(SC_OK)
                                        .withHeader(ID, randomUUID().toString())
                                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                        .withBody(payload)));
    }

    public static void verifyReferToCourtCommandSent(final JsonObject expectedCommandPayload) {
        await().until(() ->
                findAll(postRequestedFor(urlPathMatching(REFER_TO_COURT_COMMAND_URL + ".*"))
                        .withHeader(CONTENT_TYPE, WireMock.equalTo(REFER_TO_COURT_COMMAND_CONTENT)))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JsonHelper::getJsonObject)
                        .anyMatch(commandPayload -> lenientCompare(commandPayload, expectedCommandPayload)));
    }

    public static void verifyReferToCourtCommandSentStrictMode(final JsonObject expectedCommandPayload, final List<String> ignoreCompareList) {
        await().until(() ->
                findAll(postRequestedFor(urlPathMatching(REFER_TO_COURT_COMMAND_URL + ".*"))
                        .withHeader(CONTENT_TYPE, WireMock.equalTo(REFER_TO_COURT_COMMAND_CONTENT)))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JsonHelper::getJsonObject)
                        .anyMatch(commandPayload -> strictCompareIgnoringSelectedData(commandPayload, expectedCommandPayload,ignoreCompareList)));
    }
}
