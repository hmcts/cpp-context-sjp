package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;
import java.util.function.Predicate;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.json.JSONObject;

public class SchedulingStub {

    private static final String START_SJP_SESSION_URL = "/scheduling-service/command/api/rest/scheduling/start-sjp-session";
    private static final String END_SJP_SESSION_URL = "/scheduling-service/command/api/rest/scheduling/end-sjp-session";

    public static void stubStartSjpSessionCommand() {
        InternalEndpointMockUtils.stubPingFor("scheduling.command.start-sjp-session");

        stubFor(post(urlPathEqualTo(START_SJP_SESSION_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)));
    }

    public static void stubEndSjpSessionCommand() {
        InternalEndpointMockUtils.stubPingFor("scheduling.command.end-sjp-session");

        stubFor(post(urlPathEqualTo(END_SJP_SESSION_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)));
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

    private static void verifySessionStarted(final Predicate<JSONObject> commandPayloadPredicate) {
        await("scheduling.command.start-sjp-session command sent").until(() ->
                findAll(postRequestedFor(urlPathEqualTo(START_SJP_SESSION_URL)))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JSONObject::new)
                        .anyMatch(commandPayloadPredicate));
    }
}
