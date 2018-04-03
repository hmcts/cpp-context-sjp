package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.sjp.it.util.HttpClientUtil;

import java.io.StringReader;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.hamcrest.Matcher;

public class SessionHelper {

    public static void startDelegatedPowersSession(final UUID sessionId, final UUID userId, final String courtHouseOUCode) {
        final JsonObject payload = createObjectBuilder()
                .add("courtHouseOUCode", courtHouseOUCode)
                .build();
        startSession(sessionId, userId, payload);
    }

    public static JsonEnvelope startDelegatedPowersSessionAndGetSessionStartedEvent(final UUID sessionId, final UUID userId, final String courtHouseOUCode) {
        final JsonObject payload = createObjectBuilder()
                .add("courtHouseOUCode", courtHouseOUCode)
                .build();

        return getSessionStartedEvent(() -> startSession(sessionId, userId, payload));
    }

    public static void startMagistrateSession(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final String magistrate) {
        final JsonObject payload = createObjectBuilder()
                .add("courtHouseOUCode", courtHouseOUCode)
                .add("magistrate", magistrate)
                .build();
        startSession(sessionId, userId, payload);
    }

    public static JsonEnvelope startMagistrateSessionAndGetSessionStartedEvent(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final String magistrate) {
        final JsonObject payload = createObjectBuilder()
                .add("courtHouseOUCode", courtHouseOUCode)
                .add("magistrate", magistrate)
                .build();

        return getSessionStartedEvent(() -> startSession(sessionId, userId, payload));
    }

    public static void startSession(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final SessionType sessionType) {
        if (SessionType.MAGISTRATE.equals(sessionType)) {
            startMagistrateSession(sessionId, userId, courtHouseOUCode, "John Smith");
        } else {
            startDelegatedPowersSession(sessionId, userId, courtHouseOUCode);
        }
    }

    public static JsonObject getSession(final UUID sessionId, final UUID userId) {
        return getSession(sessionId, userId, withJsonPath("$.startedAt", notNullValue()));
    }

    public static JsonObject getSession(final UUID sessionId, final UUID userId, final Matcher payloadMatcher) {
        final RequestParamsBuilder requestParamsBuilder = requestParams(getReadUrl("/sessions/" + sessionId), "application/vnd.sjp.query.session+json")
                .withHeader(HeaderConstants.USER_ID, userId);
        final String payload = poll(requestParamsBuilder).until(status().is(OK), payload().isJson(payloadMatcher)).getPayload();
        return Json.createReader(new StringReader(payload)).readObject();
    }

    public static void endSession(final UUID sessionId, final UUID userId) {
        final String contentType = "application/vnd.sjp.end-session+json";
        final String url = String.format("/sessions/%s", sessionId);
        HttpClientUtil.makePostCall(userId, url, contentType, createObjectBuilder().build().toString(), ACCEPTED);
    }

    private static void startSession(final UUID sessionId, final UUID userId, final JsonObject payload) {
        final String contentType = "application/vnd.sjp.start-session+json";
        final String url = String.format("/sessions/%s", sessionId);
        HttpClientUtil.makePostCall(userId, url, contentType, payload.toString(), ACCEPTED);
    }

    private static JsonEnvelope getSessionStartedEvent(final Runnable action) {
        try (final MessageConsumerClient messageConsumer = new MessageConsumerClient()) {
            messageConsumer.startConsumer("public.sjp.session-started", "public.event");

            action.run();

            final String message = messageConsumer.retrieveMessage(5000).get();
            return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
        }
    }
}
