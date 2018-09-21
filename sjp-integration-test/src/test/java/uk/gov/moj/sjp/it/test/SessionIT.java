package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.helper.SessionHelper.migrateSession;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startMagistrateSessionAndWaitForEvent;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.verifyStartSessionIsNotCalled;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.SessionProcessor;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.stub.ReferenceDataStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;

import java.util.Optional;
import java.util.UUID;

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
        SchedulingStub.stubSessionQueryNotFound(sessionId);
    }

    @Test
    public void shouldStartAndEndDelegatedPowersSessionAndCreatePublicEventAndReplicateSessionInSchedulingContext() {
        final Optional<JsonEnvelope> sessionStartedEvent = SessionHelper.startDelegatedPowersSessionAndWaitForEvent(sessionId, userId, courtHouseOUCode, SessionProcessor.PUBLIC_SJP_SESSION_STARTED);

        assertThat(sessionStartedEvent.isPresent(), is(true));
        assertThat(sessionStartedEvent.get(), jsonEnvelope(metadata().withName(SessionProcessor.PUBLIC_SJP_SESSION_STARTED),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseOUCode)),
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

        SchedulingStub.verifyDelegatedPowersSessionStarted(sessionId, courtHouseOUCode, courtHouseName, localJusticeAreaNationalCourtCode);

        SessionHelper.endSession(sessionId, userId);
        SessionHelper.getSession(sessionId, userId, withJsonPath("$.endedAt", notNullValue()));
        SchedulingStub.verifySessionEnded(sessionId);
    }

    @Test
    public void shouldStartAndEndMagistrateSessionAndCreatePublicEventAndReplicateSessionInSchedulingContext() {
        final String magistrate = "John Smith";

        final Optional<JsonEnvelope> sessionStartedEvent = startMagistrateSessionAndWaitForEvent(sessionId, userId, courtHouseOUCode, magistrate, SessionProcessor.PUBLIC_SJP_SESSION_STARTED);

        assertThat(sessionStartedEvent.isPresent(), is(true));
        assertThat(sessionStartedEvent.get(), jsonEnvelope(metadata().withName(SessionProcessor.PUBLIC_SJP_SESSION_STARTED),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseOUCode)),
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

        SchedulingStub.verifyMagistrateSessionStarted(sessionId, courtHouseOUCode, courtHouseName, localJusticeAreaNationalCourtCode, magistrate);

        SessionHelper.endSession(sessionId, userId);
        SessionHelper.getSession(sessionId, userId, withJsonPath("$.endedAt", notNullValue()));
        SchedulingStub.verifySessionEnded(sessionId);
    }

    @Test
    public void shouldMigrateSessionAndCreatePrivateEvent() {
        final String magistrate = "John Smith";

        final JsonEnvelope sessionStartedEvent = new EventListener()
                .subscribe(MagistrateSessionStarted.EVENT_NAME)
                .run(() -> migrateSession(existingSessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate))
                .popEvent(MagistrateSessionStarted.EVENT_NAME)
                .get();

        assertThat(sessionStartedEvent, jsonEnvelope(metadata().withName(MagistrateSessionStarted.EVENT_NAME),
                payloadIsJson(allOf(
                        withJsonPath("$.magistrate", equalTo(magistrate)),
                        withJsonPath("$.sessionId", equalTo(existingSessionId.toString())),
                        withJsonPath("$.userId", equalTo(userId.toString())),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.startedAt", equalTo(startedAt))
                ))));

        SessionHelper.getSession(existingSessionId, userId, withJsonPath("$.startedAt", equalTo(startedAt)));
        verifyStartSessionIsNotCalled(existingSessionId);
    }

}
