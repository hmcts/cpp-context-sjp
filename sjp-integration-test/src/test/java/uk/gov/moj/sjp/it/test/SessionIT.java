package uk.gov.moj.sjp.it.test;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.helper.SessionHelper.MAGISTRATE_SESSION_STARTED_EVENT;
import static uk.gov.moj.sjp.it.helper.SessionHelper.SESSION_STARTED_PUBLIC_EVENT;
import static uk.gov.moj.sjp.it.helper.SessionHelper.migrateSession;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startDelegatedPowersSession;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startMagistrateSession;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.verifyStartSessionIsNotCalled;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.stub.ReferenceDataStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;

import java.util.UUID;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;


public class SessionIT extends BaseIntegrationTest {

    private final UUID existingSessionId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String courtHouseOUCode = "B01OK";
    private final String courtHouseName = "Wimbledon Magistrates' Court";
    private final String localJusticeAreaNationalCourtCode = "2577";
    private final String startedAt = new UtcClock().now().minusMonths(6).toString();

    @Before
    public void init() {
        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(courtHouseOUCode, localJusticeAreaNationalCourtCode, courtHouseName);
        SchedulingStub.stubStartSjpSessionCommand();
        SchedulingStub.stubEndSjpSessionCommand();
        SchedulingStub.stubSessionQuery(existingSessionId);
        SchedulingStub.stubSessionQueryNotFound();
    }

    @Test
    public void shouldStartAndEndDelegatedPowersSessionAndCreatePublicEventAndReplicateSessionInSchedulingContext() {
        final JsonEnvelope sessionStartedEvent = new EventListener()
                .subscribe(SESSION_STARTED_PUBLIC_EVENT)
                .run(() -> startDelegatedPowersSession(sessionId, userId, courtHouseOUCode))
                .popEvent(SESSION_STARTED_PUBLIC_EVENT)
                .get();

        assertThat(sessionStartedEvent, jsonEnvelope(metadata().withName("public.sjp.session-started"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", equalTo(DELEGATED_POWERS.name())))
                )));

        assertThat(SessionHelper.getSession(sessionId, userId).toString(), isJson(allOf(
                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                withJsonPath("$.userId", equalTo(userId.toString())),
                withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                withJsonPath("$.type", equalTo(DELEGATED_POWERS.name())),
                withoutJsonPath("$.endedAt"),
                withoutJsonPath("$.magistrate")
        )));

        SchedulingStub.verifyDelegatedPowersSessionStarted(sessionId, courtHouseName, localJusticeAreaNationalCourtCode);

        SessionHelper.endSession(sessionId, userId);
        SessionHelper.getSession(sessionId, userId, withJsonPath("$.endedAt", notNullValue()));
        SchedulingStub.verifySessionEnded(sessionId);
    }

    @Test
    public void shouldStartAndEndMagistrateSessionAndCreatePublicEventAndReplicateSessionInSchedulingContext() {

        final String magistrate = "John Smith";

        final JsonEnvelope sessionStartedEvent = new EventListener()
                .subscribe(SESSION_STARTED_PUBLIC_EVENT)
                .run(() -> startMagistrateSession(sessionId, userId, courtHouseOUCode, magistrate))
                .popEvent(SESSION_STARTED_PUBLIC_EVENT)
                .get();

        assertThat(sessionStartedEvent, jsonEnvelope(metadata().withName("public.sjp.session-started"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", equalTo(MAGISTRATE.name())))
                )));

        assertThat(SessionHelper.getSession(sessionId, userId).toString(), isJson(allOf(
                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                withJsonPath("$.userId", equalTo(userId.toString())),
                withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                withJsonPath("$.type", equalTo(MAGISTRATE.name())),
                withJsonPath("$.magistrate", equalTo(magistrate))
        )));

        SchedulingStub.verifyMagistrateSessionStarted(sessionId, courtHouseName, localJusticeAreaNationalCourtCode, magistrate);

        SessionHelper.endSession(sessionId, userId);
        SessionHelper.getSession(sessionId, userId, withJsonPath("$.endedAt", notNullValue()));
        SchedulingStub.verifySessionEnded(sessionId);
    }

    @Test
    public void shouldMigrateSessionAndCreatePrivateEvent() {
        final String magistrate = "John Smith";

        final JsonEnvelope sessionStartedEvent = new EventListener()
                .subscribe(MAGISTRATE_SESSION_STARTED_EVENT)
                .run(() -> migrateSession(existingSessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate))
                .popEvent(MAGISTRATE_SESSION_STARTED_EVENT)
                .get();

        assertThat(sessionStartedEvent, jsonEnvelope(metadata().withName("sjp.events.magistrate-session-started"),
                payloadIsJson(allOf(
                        withJsonPath("$.magistrate", equalTo(magistrate)),
                        withJsonPath("$.sessionId", equalTo(existingSessionId.toString())),
                        withJsonPath("$.userId", equalTo(userId.toString())),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.startedAt", equalTo(startedAt))
                ))));

        SessionHelper.getSession(existingSessionId, userId, withJsonPath("$.startedAt", equalTo(startedAt)));
        verifyStartSessionIsNotCalled();

    }

}
