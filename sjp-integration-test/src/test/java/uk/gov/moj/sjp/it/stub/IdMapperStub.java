package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;

import java.time.ZonedDateTime;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.client.WireMock;

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
    }

    public static void stubForIdMapperSuccess(final Response.Status status) {
        stubForIdMapperSuccess(status, UUID.randomUUID());
    }

    public static void stubAddMapping() {
        stubFor(post(urlPathMatching("/system-id-mapper-api/rest/systemid/mappings.*")).willReturn(WireMock.aResponse().withStatus(200).withHeader("CPPID", UUID.randomUUID().toString()).withBody(JsonObjects.createObjectBuilder().add("id", UUID.randomUUID().toString()).add("code", "OK").build().toString())));
    }

}