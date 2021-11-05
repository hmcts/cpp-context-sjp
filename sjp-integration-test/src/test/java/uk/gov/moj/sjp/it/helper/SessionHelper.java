package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LEGAL_ADVISER;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.sjp.it.util.HttpClientUtil;

import java.io.StringReader;
import java.util.EnumMap;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class SessionHelper {

    private static final EnumMap<SessionType, String> SESSION_CREATED_EVENT_NAME_BY_SESSION_TYPE = new EnumMap(SessionType.class);

    static {
        SESSION_CREATED_EVENT_NAME_BY_SESSION_TYPE.put(SessionType.MAGISTRATE, MagistrateSessionStarted.EVENT_NAME);
        SESSION_CREATED_EVENT_NAME_BY_SESSION_TYPE.put(SessionType.DELEGATED_POWERS, DelegatedPowersSessionStarted.EVENT_NAME);
    }

    public static UUID startDelegatedPowersSessionAsync(final UUID sessionId, final UUID userId, final String courtHouseOUCode) {
        final JsonObject payload = createObjectBuilder()
                .add("courtHouseOUCode", courtHouseOUCode)
                .build();
        return startSession(sessionId, userId, payload);
    }

    public static Optional<JsonEnvelope> startDelegatedPowersSessionAndWaitForEvent(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final String eventName) {
        return new EventListener().subscribe(eventName)
                .run(() -> startDelegatedPowersSessionAsync(sessionId, userId, courtHouseOUCode))
                .popEvent(eventName);
    }

    public static Optional<JsonEnvelope> startDelegatedPowersSession(final UUID sessionId, final UUID userId, final String courtHouseOUCode) {
        return startDelegatedPowersSessionAndWaitForEvent(sessionId, userId, courtHouseOUCode, DelegatedPowersSessionStarted.EVENT_NAME);
    }

    public static UUID startMagistrateSessionAsync(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final String magistrate, final DelegatedPowers legalAdviser) {
        final JsonObject payload = createObjectBuilder()
                .add("courtHouseOUCode", courtHouseOUCode)
                .add("magistrate", magistrate)
                .add("legalAdviser", buildLegalAdviserJsonObject(legalAdviser))
                .build();

        return startSession(sessionId, userId, payload);
    }

    public static Optional<JsonEnvelope> startMagistrateSessionAndWaitForEvent(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final String magistrate, final String eventName) {
        return new EventListener().subscribe(eventName)
                .run(() -> startMagistrateSessionAsync(sessionId, userId, courtHouseOUCode, magistrate, DEFAULT_LEGAL_ADVISER))
                .popEvent(eventName);
    }

    public static Optional<JsonEnvelope> startMagistrateSession(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final String magistrate) {
        return startMagistrateSessionAndWaitForEvent(sessionId, userId, courtHouseOUCode, magistrate, MagistrateSessionStarted.EVENT_NAME);
    }

    public static void startSessionAsync(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final SessionType sessionType) {
        switch (sessionType) {
            case MAGISTRATE:
                startMagistrateSessionAsync(sessionId, userId, courtHouseOUCode, "John Smith " + sessionId, DEFAULT_LEGAL_ADVISER);
                break;
            case DELEGATED_POWERS:
                startDelegatedPowersSessionAsync(sessionId, userId, courtHouseOUCode);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Creation session of type {} is not supported", sessionType));
        }
    }

    public static Optional<JsonEnvelope> startSession(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final SessionType sessionType) {
        return startSessionAndWaitForEvent(sessionId, userId, courtHouseOUCode, sessionType, SESSION_CREATED_EVENT_NAME_BY_SESSION_TYPE.get(sessionType));
    }

    public static Optional<JsonEnvelope> startSessionAndWaitForEvent(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final SessionType sessionType, final String eventName) {
        return new EventListener().subscribe(eventName)
                .run(() -> startSessionAsync(sessionId, userId, courtHouseOUCode, sessionType))
                .popEvent(eventName);
    }

    public static JsonObject getSession(final UUID sessionId, final UUID userId) {
        return getSession(sessionId, userId, withJsonPath("$.startedAt", notNullValue()));
    }

    public static JsonObject getSession(final UUID sessionId, final UUID userId, final Matcher<? super ReadContext> payloadMatcher) {
        final RequestParamsBuilder requestParamsBuilder = requestParams(getReadUrl("/sessions/" + sessionId), "application/vnd.sjp.query.session+json")
                .withHeader(HeaderConstants.USER_ID, userId);
        final String payload = pollWithDefaults(requestParamsBuilder)
                .until(status().is(OK), payload().isJson(payloadMatcher)).getPayload();
        return Json.createReader(new StringReader(payload)).readObject();
    }

    public static UUID endSession(final UUID sessionId, final UUID userId) {
        final String contentType = "application/vnd.sjp.end-session+json";
        final String url = String.format("/sessions/%s", sessionId);
        return HttpClientUtil.makePostCall(userId, url, contentType, createObjectBuilder().build().toString(), ACCEPTED);
    }

    private static UUID startSession(final UUID sessionId, final UUID userId, final JsonObject payload) {
        final String contentType = "application/vnd.sjp.start-session+json";
        final String url = String.format("/sessions/%s", sessionId);
        return HttpClientUtil.makePostCall(userId, url, contentType, payload.toString(), ACCEPTED);
    }

    private static JsonObject buildLegalAdviserJsonObject(final DelegatedPowers legalAdviser) {
        return createObjectBuilder()
                .add("firstName", legalAdviser.getFirstName())
                .add("lastName", legalAdviser.getLastName())
                .add("userId", legalAdviser.getUserId().toString())
                .build();
    }
}
