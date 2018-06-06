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
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.sjp.it.util.HttpClientUtil;

import java.io.StringReader;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.base.Strings;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class SessionHelper {

    public static final String SESSION_STARTED_PUBLIC_EVENT = "public.sjp.session-started";
    public static final String MAGISTRATE_SESSION_STARTED_EVENT = "sjp.events.magistrate-session-started";
    public static final String DELEGATED_POWERS_SESSION_STARTED_EVENT = "sjp.events.delegated-powers-session-started";
    public static final String DELEGATED_POWERS_SESSION_ENDED_EVENT = DelegatedPowersSessionEnded.EVENT_NAME;

    public static UUID startDelegatedPowersSession(final UUID sessionId, final UUID userId, final String courtHouseOUCode) {
        final JsonObject payload = createObjectBuilder()
                .add("courtHouseOUCode", courtHouseOUCode)
                .build();
        return startSession(sessionId, userId, payload);
    }

    public static UUID startMagistrateSession(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final String magistrate) {
        final JsonObject payload = createObjectBuilder()
                .add("courtHouseOUCode", courtHouseOUCode)
                .add("magistrate", magistrate)
                .build();

        return startSession(sessionId, userId, payload);
    }

    public static UUID startSession(final UUID sessionId, final UUID userId, final String courtHouseOUCode, final SessionType sessionType) {
        if (SessionType.MAGISTRATE.equals(sessionType)) {
            return startMagistrateSession(sessionId, userId, courtHouseOUCode, "John Smith");
        } else {
            return startDelegatedPowersSession(sessionId, userId, courtHouseOUCode);
        }
    }

    public static JsonObject getSession(final UUID sessionId, final UUID userId) {
        return getSession(sessionId, userId, withJsonPath("$.startedAt", notNullValue()));
    }

    public static JsonObject getSession(final UUID sessionId, final UUID userId, final Matcher<? super ReadContext> payloadMatcher) {
        final RequestParamsBuilder requestParamsBuilder = requestParams(getReadUrl("/sessions/" + sessionId), "application/vnd.sjp.query.session+json")
                .withHeader(HeaderConstants.USER_ID, userId);
        final String payload = poll(requestParamsBuilder).until(status().is(OK), payload().isJson(payloadMatcher)).getPayload();
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

    public static UUID migrateSession(final UUID sessionId, final UUID userId, final String courthouseName, final String localJusticeAreaNationalCourtCode, final String startedAt) {
        return migrateSession(sessionId, userId, courthouseName, localJusticeAreaNationalCourtCode, startedAt, "");
    }

    public static UUID migrateSession(final UUID sessionId,
                                      final UUID userId,
                                      final String courtHouseName,
                                      final String localJusticeAreaNationalCourtCode,
                                      final String startedAt,
                                      final String magistrate) {

        JsonObjectBuilder sessionBuilder = createObjectBuilder()
                .add("sessionId", sessionId.toString())
                .add("userId", userId.toString())
                .add("courtHouseName", courtHouseName)
                .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                .add("startedAt", startedAt);

        if (!Strings.isNullOrEmpty(magistrate)) {
            sessionBuilder.add("magistrate", magistrate);
        }

        final JsonObject payload = sessionBuilder.build();

        final String contentType = "application/vnd.sjp.migrate-session+json";
        final String url = String.format("/sessions/%s", sessionId);
        return HttpClientUtil.makePostCall(userId, url, contentType, payload.toString(), ACCEPTED);
    }

}
