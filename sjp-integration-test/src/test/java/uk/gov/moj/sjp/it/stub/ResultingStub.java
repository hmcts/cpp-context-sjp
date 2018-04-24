package uk.gov.moj.sjp.it.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

import javax.json.Json;

public class ResultingStub {

    private static final String RESULTING_QUERY_URL = "/resulting-service/query/api/rest/resulting/cases/%s/case-decisions";
    private static final String RESULTING_QUERY_MEDIA_TYPE = "application/vnd.resulting.query.case-decisions+json";

    public static void stubGetCaseDecisionsWithNoDecision(final UUID caseId) {
        InternalEndpointMockUtils.stubPingFor("resulting-service");

        final String responsePayload = Json.createObjectBuilder()
                .add("caseDecisions", Json.createArrayBuilder())
                .build()
                .toString();

        String url = String.format(RESULTING_QUERY_URL, caseId);

        stubFor(get(urlPathMatching(url))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayload)));

        waitForStubToBeReady(url, RESULTING_QUERY_MEDIA_TYPE);
    }

    public static void stubGetCaseDecisionsWithDecision(final UUID caseId) {
        InternalEndpointMockUtils.stubPingFor("resulting-service");

        String url = String.format(RESULTING_QUERY_URL, caseId);

        stubFor(get(urlPathMatching(url))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/resulting.query.case-decisions.json"))));

        waitForStubToBeReady(url, RESULTING_QUERY_MEDIA_TYPE);
    }
}
