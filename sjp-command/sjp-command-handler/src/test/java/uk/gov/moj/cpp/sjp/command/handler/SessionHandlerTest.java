package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
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

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.SessionStarted;

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
    private static final String MAGISTRATE_SESSION_STARTED_EVENT = "sjp.events.magistrate-session-started";
    private static final String DELEGATED_POWERS_SESSION_STARTED_EVENT = "sjp.events.delegated-powers-session-started";

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream sessionEventStream;

    @Mock
    private Session session;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(MagistrateSessionStarted.class, DelegatedPowersSessionStarted.class);

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @InjectMocks
    private SessionHandler sessionHandler;

    private UUID sessionId, legalAdviserId;
    private String courtCode;
    private ZonedDateTime sessionStartTime;

    @Before
    public void init() {
        sessionId = randomUUID();
        legalAdviserId = randomUUID();
        courtCode = randomNumeric(5);
        sessionStartTime = clock.now();
    }

    @Test
    public void shouldCreateMagistrateSession() throws EventStreamException {

        final String magistrate = randomAlphanumeric(20);

        final JsonEnvelope startSessionCommand = envelopeFrom(metadataWithRandomUUID(START_SESSION_COMMAND).withUserId(legalAdviserId.toString()),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("magistrate", magistrate)
                        .add("courtCode", courtCode)
                        .build());


        final MagistrateSessionStarted sessionStartedEvent = new MagistrateSessionStarted(sessionId, legalAdviserId, courtCode, magistrate, sessionStartTime);

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.startSession(sessionId, legalAdviserId, courtCode, magistrate, sessionStartTime)).thenReturn(Stream.of(sessionStartedEvent));

        sessionHandler.startSession(startSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(startSessionCommand)
                                        .withName(MAGISTRATE_SESSION_STARTED_EVENT),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.legalAdviserId", equalTo(legalAdviserId.toString())),
                                        withJsonPath("$.courtCode", equalTo(courtCode)),
                                        withJsonPath("$.magistrate", equalTo(magistrate)),
                                        withJsonPath("$.startedAt", equalTo(sessionStartTime.toString()))
                                ))))));
    }

    @Test
    public void shouldCreateDelegatedPowersSession() throws EventStreamException {

        final JsonEnvelope startSessionCommand = envelopeFrom(metadataWithRandomUUID(START_SESSION_COMMAND).withUserId(legalAdviserId.toString()),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("courtCode", courtCode)
                        .build());

        final DelegatedPowersSessionStarted sessionStartedEvent = new DelegatedPowersSessionStarted(sessionId, legalAdviserId, courtCode, sessionStartTime);

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.startSession(sessionId, legalAdviserId, courtCode, null, sessionStartTime)).thenReturn(Stream.of(sessionStartedEvent));

        sessionHandler.startSession(startSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(startSessionCommand)
                                        .withName(DELEGATED_POWERS_SESSION_STARTED_EVENT),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.legalAdviserId", equalTo(legalAdviserId.toString())),
                                        withJsonPath("$.courtCode", equalTo(courtCode)),
                                        withJsonPath("$.startedAt", equalTo(sessionStartTime.toString())),
                                        withoutJsonPath("$.magistrate")
                                ))))));
    }

    @Test
    public void shouldHandleStartSessionCommand() {
        assertThat(SessionHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("startSession").thatHandles(START_SESSION_COMMAND)));
    }
}
