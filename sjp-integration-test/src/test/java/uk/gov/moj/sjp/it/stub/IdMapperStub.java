package uk.gov.moj.sjp.it.stub;

import com.github.tomakehurst.wiremock.client.WireMock;

import javax.json.Json;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.moj.sjp.it.stub.StubHelper.waitForGetStubToBeReady;
import static uk.gov.moj.sjp.it.stub.StubHelper.waitForPostStubToBeReady;

public class IdMapperStub {

    public static void stubGetFromIdMapper(final String sourceType, final String sourceId, final String targetType, final String targetId) {

        final String responseBody = createObjectBuilder()
                .add("mappingId", UUID.randomUUID().toString())
                .add("sourceId", sourceId)
                .add("sourceType", sourceType)
                .add("targetId", targetId)
                .add("targetType", targetType)
                .add("createdAt", ZonedDateTime.now().toString()).build().toString();

        stubFor(get(urlPathMatching("/system-id-mapper-api/rest/systemid/mappings.*"))
                .withHeader("Accept", equalTo("application/vnd.systemid.mapping+json"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withBody(responseBody)
                )
        );

        waitForGetStubToBeReady(format("/system-id-mapper-api/rest/systemid/mappings?sourceId=%s", sourceId), "application/vnd.systemid.mapping+json", OK);
    }

    public static void stubCommandForRequestResponse() {
        stubFor(post(urlMatching(format("/system-id-mapper-api/rest/systemid/mappings.*")))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader(CONTENT_TYPE, "application/vnd.systemid.map+json")
                        .withBody("")));
    }

    public static void stubForIdMapperSuccess(final Response.Status status, final UUID id) {
        final String path = "/system-id-mapper-api/rest/systemid.*";
        final String mime = "application/vnd.systemid.map+json";

        stubFor(post(urlPathMatching(path))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(mime))
                .willReturn(aResponse()
                        .withStatus(status.getStatusCode())
                        .withBody(createObjectBuilder().add("id", id.toString()).build().toString())
                )
        );

        waitForPostStubToBeReady(path, mime, status);
    }

    public static void stubForIdMapperSuccess(final Response.Status status) {
        stubForIdMapperSuccess(status, UUID.randomUUID());
    }

    private static final String ID_MAPPER_QUERY_URL = "/system-id-mapper-api/rest/systemid/mappings.*";
    private static final String ID_MAPPER_QUERY_MEDIA_TYPE = "application/vnd.systemid.map+json";
    private static final String ID_MAPPER_SERVICE = "system-id-mapper-api";

    public static void stubIdMapperMappingFor(final int statusToReturn, final String sourceId, final String sourceType, final UUID targetId, final String targetType) {
        final String error = format("Insert of mapping %s:%s to %s:%s failed due to conflict.", sourceId, sourceType, targetId.toString(), targetType);
        final String responseConflictPayload = createObjectBuilder()
                .add("id", targetId.toString())
                .add("error", error).build().toString();

        final String responseNoConflictPayload =  createObjectBuilder().add("id", targetId.toString()).build().toString();

        stubPingFor(ID_MAPPER_SERVICE);

        final String response = statusToReturn == 200 ? responseNoConflictPayload : responseConflictPayload;
        final String mapObj = createObjectBuilder()
                .add("sourceId", sourceId)
                .add("sourceType", sourceType)
                .add("targetType", targetType).build().toString();

        stubFor(any(urlPathMatching(ID_MAPPER_QUERY_URL))
                .withHeader(CONTENT_TYPE, equalTo(ID_MAPPER_QUERY_MEDIA_TYPE))
                .willReturn(aResponse()
                        .withStatus(statusToReturn)
                        .withBody(response)
                ));


    }

    public static void stubAddMapping() {
        stubFor(post(urlPathMatching("/system-id-mapper-api/rest/systemid/mappings.*")).willReturn(WireMock.aResponse().withStatus(200).withHeader("CPPID", UUID.randomUUID().toString()).withBody(Json.createObjectBuilder().add("id", UUID.randomUUID().toString()).add("code", "OK").build().toString())));
    }

}