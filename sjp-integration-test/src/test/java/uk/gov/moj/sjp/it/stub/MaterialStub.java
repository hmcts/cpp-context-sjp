package uk.gov.moj.sjp.it.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

public class MaterialStub extends StubUtil {

    public static final String QUERY_URL = "/material-command-api/command/api/rest/material/material";
    public static final String QUERY_MEDIA_TYPE = "application/vnd.material.command.upload-file+json";

    public static void stubAddCaseMaterial() {
        InternalEndpointMockUtils.stubPingFor("material-command-api");

        stubFor(post(urlPathEqualTo(QUERY_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", DEFAULT_JSON_CONTENT_TYPE)));

        stubFor(get(urlPathEqualTo(QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(QUERY_URL, QUERY_MEDIA_TYPE);
    }
}
