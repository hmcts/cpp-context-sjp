package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import javax.json.Json;
import javax.json.JsonObject;

public class ReferenceDataStub {

    public static void stubQueryOffences(String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-query-api");
        final JsonObject offences = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-query-api/query/api/rest/referencedata/offences";
        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("cjsoffencecode", matching(".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(offences.toString())));

        waitForStubToBeReady(urlPath + "?cjsoffencecode", "application/vnd.referencedata.query.offences+json");
    }
}
