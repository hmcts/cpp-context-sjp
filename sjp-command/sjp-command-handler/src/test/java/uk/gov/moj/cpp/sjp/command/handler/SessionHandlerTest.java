package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.SessionEnded;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionHandlerTest {

    private static final String START_SESSION_COMMAND = "sjp.command.start-session";
    private static final String END_SESSION_COMMAND = "sjp.command.end-session";

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream sessionEventStream;

    @Mock
    private Session session;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            MagistrateSessionStarted.class,
            MagistrateSessionEnded.class,
            DelegatedPowersSessionStarted.class,
            DelegatedPowersSessionEnded.class);

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @InjectMocks
    private SessionHandler sessionHandler;

    private UUID sessionId, userId;
    private String courtHouseName, localJusticeAreaNationalCourtCode;
    private ZonedDateTime startedAt;
    private ZonedDateTime endedAt;

    @Before
    public void init() {
        sessionId = randomUUID();
        userId = randomUUID();
        courtHouseName = "Coventry Magistrates' Court";
        localJusticeAreaNationalCourtCode = "2924";
        startedAt = clock.now();
        endedAt = clock.now();
    }

    @Test
    public void shouldHandleSessionCommands() {
        assertThat(SessionHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(allOf(
                        method("startSession").thatHandles(START_SESSION_COMMAND),
                        method("endSession").thatHandles(END_SESSION_COMMAND)
                )));
    }

    @Test
    public void shouldStartMagistrateSession() throws EventStreamException {

        final String magistrate = randomAlphanumeric(20);

        final JsonEnvelope startSessionCommand = envelopeFrom(metadataWithRandomUUID(START_SESSION_COMMAND).withUserId(userId.toString()),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("magistrate", magistrate)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .build());


        final MagistrateSessionStarted sessionStartedEvent = new MagistrateSessionStarted(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate);

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.startMagistrateSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate)).thenReturn(Stream.of(sessionStartedEvent));

        sessionHandler.startSession(startSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(startSessionCommand)
                                        .withName(MagistrateSessionStarted.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.userId", equalTo(userId.toString())),
                                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                                        withJsonPath("$.magistrate", equalTo(magistrate)),
                                        withJsonPath("$.startedAt", equalTo(startedAt.toString()))
                                ))))));
    }

    @Test
    public void shouldStartDelegatedPowersSession() throws EventStreamException {

        final JsonEnvelope startSessionCommand = envelopeFrom(metadataWithRandomUUID(START_SESSION_COMMAND).withUserId(userId.toString()),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .build());

        final DelegatedPowersSessionStarted sessionStartedEvent = new DelegatedPowersSessionStarted(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.startDelegatedPowersSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt)).thenReturn(Stream.of(sessionStartedEvent));

        sessionHandler.startSession(startSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(startSessionCommand)
                                        .withName(DelegatedPowersSessionStarted.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.userId", equalTo(userId.toString())),
                                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                                        withJsonPath("$.startedAt", equalTo(startedAt.toString())),
                                        withoutJsonPath("$.magistrate")
                                ))))));
    }

    @Test
    public void shouldEndDelegatedPowersSession() throws EventStreamException {
        shouldEndSession(new DelegatedPowersSessionEnded(sessionId, endedAt));
    }

    @Test
    public void shouldEndMagistrateSession() throws EventStreamException {
        shouldEndSession(new MagistrateSessionEnded(sessionId, endedAt));
    }

    private void shouldEndSession(final SessionEnded sessionEnded) throws EventStreamException {
        final JsonEnvelope endSessionCommand = envelopeFrom(metadataWithRandomUUID(END_SESSION_COMMAND),
                createObjectBuilder().add("sessionId", sessionEnded.getSessionId().toString()).build());

        when(eventSource.getStreamById(sessionEnded.getSessionId())).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.endSession(sessionEnded.getSessionId(), sessionEnded.getEndedAt())).thenReturn(Stream.of(sessionEnded));

        sessionHandler.endSession(endSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(endSessionCommand)
                                        .withName(sessionEnded.getClass().getAnnotation(Event.class).value()),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionEnded.getSessionId().toString())),
                                        withJsonPath("$.endedAt", equalTo(sessionEnded.getEndedAt().toString()))
                                ))))));
    }

}
