package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

public class ProsecutionCaseFileServiceStub {

    public static void stubCaseDetails(final UUID caseId, final UUID defendantId, final UUID offenceId, final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("prosecutioncasefile-service");

        final String query = "application/vnd.prosecutioncasefile.query.case+json";
        final String urlPath = format("/prosecutioncasefile-service/query/api/rest/prosecutioncasefile/cases/%s", caseId.toString());
        String payload = getPayload(resourceName);
        payload = payload.replace("51cac7fb-387c-4d19-9c80-8963fa8cf228", caseId.toString());
        payload = payload.replace("4a1e66ab-8673-4300-aed8-b2391e38d8db", defendantId.toString());
        payload = payload.replace("63f61b32-0fd0-4a76-bab1-ee68fb54e93f", offenceId.toString());

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(query))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(payload)));

        waitForStubToBeReady(urlPath, query);
    }
}
