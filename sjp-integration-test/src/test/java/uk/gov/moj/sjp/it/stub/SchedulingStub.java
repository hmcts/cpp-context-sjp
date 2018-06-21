package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Predicate;

import javax.json.Json;
import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.json.JSONObject;

public class SchedulingStub {

    private static final String START_SJP_SESSION_URL = "/scheduling-service/command/api/rest/scheduling/start-sjp-session";
    private static final String END_SJP_SESSION_URL = "/scheduling-service/command/api/rest/scheduling/end-sjp-session";

    private static final String READ_SJP_SESSION_SPECIFIC_URL = "/scheduling-service/query/api/rest/scheduling/sjp-sessions/%s";

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

    public static void stubSessionQueryNotFound(final UUID sessionId) {
        InternalEndpointMockUtils.stubPingFor("scheduling.query.sjp-session");

        stubFor(get(urlPathEqualTo(format(READ_SJP_SESSION_SPECIFIC_URL, sessionId)))
                .willReturn(aResponse().withStatus(SC_NOT_FOUND)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubSessionQuery(final UUID sessionId) {
        InternalEndpointMockUtils.stubPingFor("scheduling.query.sjp-session");

        final JsonObject session = Json.createObjectBuilder()
                .add("id", sessionId.toString())
                .add("userId", randomUUID().toString())
                .add("started", ZonedDateTime.now().toString())
                .add("nationalCourtCode", "2577")
                .add("courtLocation", "Battersea")
                .build();

        stubFor(get(urlPathEqualTo(format(READ_SJP_SESSION_SPECIFIC_URL, sessionId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(session.toString())));
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

    public static void verifyStartSessionIsNotCalled(final UUID sessionId) {
        findAll(postRequestedFor(urlPathEqualTo(START_SJP_SESSION_URL)))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(JSONObject::new)
                .noneMatch(commandPayload -> commandPayload.getString("id").equals(sessionId.toString()));
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
