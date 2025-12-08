package uk.gov.moj.sjp.it.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.POLL_INTERVAL;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import uk.gov.justice.services.test.utils.core.http.FibonacciPollWithStartAndMax;

public class MaterialStub {

    public static final String COMMAND_URL = "/material-service/command/api/rest/material/material";
    public static final String DELETE_COMMAND_URL = "/material-service/command/api/rest/material/material/%s";
    public static final String MATERIAL_QUERY_URL = "/material-service/query/api/rest/material";
    public static final String COMMAND_MEDIA_TYPE = "application/vnd.material.command.upload-file+json";
    public static final String MATERIAL_METADATA_QUERY_MEDIA_TYPE = "application/vnd.material.query.material-metadata+json";

    public static void stubAddCaseMaterial() {
        stubFor(post(urlPathEqualTo(COMMAND_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        stubFor(get(urlPathEqualTo(COMMAND_URL))
                .willReturn(aResponse().withStatus(SC_OK)));
    }

    public static UUID processMaterialAddedCommand(final UUID documentReference) {
        final DefaultJsonObjectEnvelopeConverter envelopeConverter = new DefaultJsonObjectEnvelopeConverter();
        final JsonEnvelope addMaterialCommand = await().until(() -> findAll(postRequestedFor(urlPathEqualTo(COMMAND_URL))
                        .withHeader(CONTENT_TYPE, equalTo(COMMAND_MEDIA_TYPE)))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(envelopeConverter::asEnvelope)
                        .filter(command -> documentReference.toString().equals(command.payloadAsJsonObject().getString("fileServiceId", null)))
                        .findFirst(), not(empty()))
                .get();

        JmsMessageProducerClient messageProducerClient = newPublicJmsMessageProducerClientProvider()
                .getMessageProducerClient();

        final JsonEnvelope materialAddedEventPayload = envelopeFrom(metadataFrom(addMaterialCommand.metadata())
                        .withName("material.material-added"),
                createObjectBuilder()
                        .add("materialId", addMaterialCommand.payloadAsJsonObject().getJsonString("materialId"))
                        .add("fileDetails", createObjectBuilder()
                                .add("fileName", "fileName")
                                .add("externalLink", "localhost/fileName").build())
                        .add("materialAddedDate", ZonedDateTime.now().toString())
                        .build());

        messageProducerClient.sendMessage("material.material-added", materialAddedEventPayload);

        return UUID.fromString(addMaterialCommand.payloadAsJsonObject().getString("materialId"));
    }

    public static void stubMaterialMetadata(final UUID materialId, final String fileName, final String mimeType, final ZonedDateTime addedAt) {
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
        stubFor(get(urlPathEqualTo(MATERIAL_QUERY_URL + "/material/" + materialId + "/metadata"))
                .willReturn(aResponse().withStatus(responseStatusCode)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, MATERIAL_METADATA_QUERY_MEDIA_TYPE)));
    }

    public static void stubMaterialContent(final UUID materialId, final byte[] materialContent, final String mimeType) {
        stubFor(get(urlPathEqualTo(MATERIAL_QUERY_URL + "/material/" + materialId))
                .withQueryParam("stream", equalTo("true"))
                .withQueryParam("requestPdf", equalTo("false"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, mimeType)
                        .withBody(materialContent)));
    }

    public static void stubMaterialContentWithResponseStatusCode(final UUID materialId, int responseStatusCode) {
        stubFor(get(urlPathEqualTo(MATERIAL_QUERY_URL + "/material/" + materialId))
                .withQueryParam("stream", equalTo("true"))
                .withQueryParam("requestPdf", equalTo("false"))
                .willReturn(aResponse().withStatus(responseStatusCode)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())));
    }

    public static void stubDeleteMaterial(final String materialId) {
        stubFor(post(urlPathEqualTo(format(DELETE_COMMAND_URL, materialId)))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)));
    }

    public static void assertMaterialDeleteFMICommandInvoked(String materialId) {
        verifyCallsToStubbedEndpoint(format(DELETE_COMMAND_URL, materialId), 1);
    }


    public static void verifyCallsToStubbedEndpoint(final String url, final int numberOfCalls) {
        await().timeout(30, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL)
                .until(
                        () -> findAll(postRequestedFor(urlMatching(url))).size(), is(numberOfCalls)
                );
    }
}
