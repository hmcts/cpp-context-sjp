package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.awaitility.Awaitility.await;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;
import java.util.function.Predicate;

import javax.json.Json;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.json.JSONObject;

public class SchedulingStub {

    private static final String START_SJP_SESSION_URL = "/scheduling-service/command/api/rest/scheduling/start-sjp-session";
    private static final String END_SJP_SESSION_URL = "/scheduling-service/command/api/rest/scheduling/end-sjp-session";

    private static final String READ_SJP_SESSION_SPECIFIC_URL = "/scheduling-query-api/query/api/rest/scheduling/sjp-sessions/%s";
    private static final String READ_SJP_SESSION_BROAD_URL = "/scheduling-query-api/query/api/rest/scheduling/sjp-sessions/.*";

    public static void stubStartSjpSessionCommand() {
        InternalEndpointMockUtils.stubPingFor("scheduling.command.start-sjp-session");

        stubFor(post(urlPathEqualTo(START_SJP_SESSION_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubEndSjpSessionCommand() {
        InternalEndpointMockUtils.stubPingFor("scheduling.command.end-sjp-session");

        stubFor(post(urlPathEqualTo(END_SJP_SESSION_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubSessionQueryNotFound() {
        InternalEndpointMockUtils.stubPingFor("scheduling.query.sjp-session");

        stubFor(get(urlPathMatching(READ_SJP_SESSION_BROAD_URL))
                .willReturn(aResponse().withStatus(SC_NOT_FOUND)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubSessionQuery(UUID existingSessionId) {
        InternalEndpointMockUtils.stubPingFor("scheduling.query.sjp-session");

        stubFor(get(urlPathEqualTo(format(READ_SJP_SESSION_SPECIFIC_URL, existingSessionId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void verifyDelegatedPowersSessionStarted(final UUID sessionId, final String courtHouseName, final String localJusticeAreaNationalCourtCode) {
        final Predicate<JSONObject> commandPayloadPredicate = commandPayload -> commandPayload.getString("id").equals(sessionId.toString())
                && commandPayload.getString("courtLocation").equals(courtHouseName)
                && commandPayload.getString("nationalCourtCode").equals(localJusticeAreaNationalCourtCode)
                && !commandPayload.has("magistrate");

        verifySessionStarted(commandPayloadPredicate);
    }

    public static void verifyMagistrateSessionStarted(final UUID sessionId, final String courtHouseName, final String localJusticeAreaNationalCourtCode, final String magistrate) {
        final Predicate<JSONObject> commandPayloadPredicate = commandPayload -> commandPayload.getString("id").equals(sessionId.toString())
                && commandPayload.getString("courtLocation").equals(courtHouseName)
                && commandPayload.getString("nationalCourtCode").equals(localJusticeAreaNationalCourtCode)
                && commandPayload.getString("magistrate").equals(magistrate);

        verifySessionStarted(commandPayloadPredicate);
    }

    public static void verifySessionEnded(final UUID sessionId) {
        final Predicate<JSONObject> commandPayloadPredicate = commandPayload -> commandPayload.getString("id").equals(sessionId.toString());

        await("scheduling.command.end-sjp-session command sent").until(() ->
                findAll(postRequestedFor(urlPathEqualTo(END_SJP_SESSION_URL)))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JSONObject::new)
                        .anyMatch(commandPayloadPredicate));
    }

    public static void verifyStartSessionIsNotCalled() {
        verify(0, postRequestedFor(urlEqualTo(START_SJP_SESSION_URL)));
    }

    private static void verifySessionStarted(final Predicate<JSONObject> commandPayloadPredicate) {
        await("scheduling.command.start-sjp-session command sent").until(() ->
                findAll(postRequestedFor(urlPathEqualTo(START_SJP_SESSION_URL)))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JSONObject::new)
                        .anyMatch(commandPayloadPredicate));
    }
}
