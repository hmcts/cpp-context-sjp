package uk.gov.moj.sjp.it.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.common.http.HeaderConstants;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class MaterialStub {

    public static final String COMMAND_URL = "/material-service/command/api/rest/material/material";
    public static final String MATERIAL_QUERY_URL = "/material-service/query/api/rest/material";
    public static final String COMMAND_MEDIA_TYPE = "application/vnd.material.command.upload-file+json";
    public static final String MATERIAL_METADATA_QUERY_MEDIA_TYPE = "application/vnd.material.query.material-metadata+json";

    public static void stubAddCaseMaterial() {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(post(urlPathEqualTo(COMMAND_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        stubFor(get(urlPathEqualTo(COMMAND_URL))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(COMMAND_URL, COMMAND_MEDIA_TYPE);
    }

    public static void stubMaterialMetadata(final UUID materialId, final String fileName, final String mimeType, final ZonedDateTime addedAt) {
        InternalEndpointMockUtils.stubPingFor("material-service");

        final JsonObject metadata = Json.createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileName", fileName)
                .add("mimeType", mimeType)
                .add("materialAddedDate", addedAt.toString())
                .add("alfrescoAssetId", randomUUID().toString())
                .add("materialAddedDate", addedAt.toString())
                .add("externalLink", "")
                .build();

        stubFor(get(urlPathEqualTo(MATERIAL_QUERY_URL + "/material/" + materialId + "/metadata"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, MATERIAL_METADATA_QUERY_MEDIA_TYPE)
                        .withBody(metadata.toString())));
    }

    public static void stubMaterialMetadataWithResponseStatusCode(final UUID materialId, int responseStatusCode) {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(get(urlPathEqualTo(MATERIAL_QUERY_URL + "/material/" + materialId + "/metadata"))
                .willReturn(aResponse().withStatus(responseStatusCode)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, MATERIAL_METADATA_QUERY_MEDIA_TYPE)));
    }

    public static void stubMaterialContent(final UUID materialId, final byte[] materialContent, final String mimeType) {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(get(urlPathEqualTo(MATERIAL_QUERY_URL + "/material/" + materialId))
                .withQueryParam("stream", equalTo("true"))
                .withQueryParam("requestPdf", equalTo("false"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, mimeType)
                        .withBody(materialContent)));
    }

    public static void stubMaterialContentWithResponseStatusCode(final UUID materialId, int responseStatusCode) {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(get(urlPathEqualTo(MATERIAL_QUERY_URL + "/material/" + materialId))
                .withQueryParam("stream", equalTo("true"))
                .withQueryParam("requestPdf", equalTo("false"))
                .willReturn(aResponse().withStatus(responseStatusCode)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())));
    }
}
