package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.repository.SessionRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class SessionListenerTest {

    @Mock
    private Session existingSession;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionListener sessionListener;

    @Spy
    private Clock clock = new StoppedClock(new UtcClock().now());

    @Captor
    private ArgumentCaptor<Session> sessionCaptor;

    private final UUID sessionId = randomUUID();
    private final UUID userId = randomUUID();
    private final String courtHouseName = "Hendon Magistrates' Court";
    private final String localJusticeAreaNationalCourtCode = "2571";
    private final String magistrate = "John Smith";
    private ZonedDateTime startedAt;
    private ZonedDateTime endedAt;

    @Before
    public void setup() {
        startedAt = clock.now();
        endedAt = startedAt.plusMinutes(1);
    }

    @Test
    public void shouldHandleDelegatedPowersSessionStarted() {

        final JsonEnvelope delegatedPowersSessionStarted = envelopeFrom(metadataWithRandomUUID("sjp.events.delegated-powers-session-started"),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("userId", userId.toString())
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("startedAt", startedAt.format(ISO_DATE_TIME))
                        .build());

        sessionListener.handleDelegatedPowersSessionStarted(delegatedPowersSessionStarted);

        verify(sessionRepository).save(sessionCaptor.capture());

        final Session session = sessionCaptor.getValue();

        assertThat(session.getSessionId(), equalTo(sessionId));
        assertThat(session.getUserId(), equalTo(userId));
        assertThat(session.getCourtHouseName(), equalTo(courtHouseName));
        assertThat(session.getLocalJusticeAreaNationalCourtCode(), equalTo(localJusticeAreaNationalCourtCode));
        assertThat(session.getStartedAt(), equalTo(startedAt));
        assertThat(session.getType(), equalTo(SessionType.DELEGATED_POWERS));
        assertThat(session.getMagistrate().isPresent(), equalTo(false));
        assertThat(session.getEndedAt().isPresent(), equalTo(false));
    }

    @Test
    public void shouldHandleMagistrateSessionStarted() {

        final JsonEnvelope magistrateSessionStarted = envelopeFrom(metadataWithRandomUUID("sjp.events.magistrate-session-started"),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("userId", userId.toString())
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("startedAt", startedAt.format(ISO_DATE_TIME))
                        .add("magistrate", magistrate)
                        .build());

        sessionListener.handleMagistrateSessionStarted(magistrateSessionStarted);

        verify(sessionRepository).save(sessionCaptor.capture());

        final Session session = sessionCaptor.getValue();

        assertThat(session.getSessionId(), equalTo(sessionId));
        assertThat(session.getUserId(), equalTo(userId));
        assertThat(session.getCourtHouseName(), equalTo(courtHouseName));
        assertThat(session.getLocalJusticeAreaNationalCourtCode(), equalTo(localJusticeAreaNationalCourtCode));
        assertThat(session.getStartedAt(), equalTo(startedAt));
        assertThat(session.getType(), equalTo(SessionType.MAGISTRATE));
        assertThat(session.getMagistrate().get(), equalTo(magistrate));
        assertThat(session.getEndedAt().isPresent(), equalTo(false));
    }

    @Test
    public void shouldHandleDelegatedPowersSessionEndedEvent() {

        final JsonEnvelope delegatedPowersSessionEnded = envelopeFrom(metadataWithRandomUUID("sjp.events.delegated-powers-session-ended"),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("endedAt", endedAt.format(ISO_DATE_TIME))
                        .build());

        when(sessionRepository.findBy(sessionId)).thenReturn(existingSession);

        sessionListener.handleDelegatedPowersSessionEnded(delegatedPowersSessionEnded);

        verify(existingSession).setEndedAt(endedAt);
    }

    @Test
    public void shouldHandleMagistrateSessionEndedEvent() {

        final JsonEnvelope magistrateSessionEnded = envelopeFrom(metadataWithRandomUUID("sjp.events.magistrate-session-ended"),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("endedAt", endedAt.format(ISO_DATE_TIME))
                        .build());

        when(sessionRepository.findBy(sessionId)).thenReturn(existingSession);

        sessionListener.handleMagistrateSessionEnded(magistrateSessionEnded);

        verify(existingSession).setEndedAt(endedAt);
    }

    @Test
    public void shouldHandlesQuery() {
        assertThat(SessionListener.class, isHandlerClass(Component.EVENT_LISTENER)
                .with(allOf(
                        method("handleDelegatedPowersSessionStarted").thatHandles("sjp.events.delegated-powers-session-started"),
                        method("handleMagistrateSessionStarted").thatHandles("sjp.events.magistrate-session-started"),
                        method("handleDelegatedPowersSessionEnded").thatHandles("sjp.events.delegated-powers-session-ended"),
                        method("handleMagistrateSessionEnded").thatHandles("sjp.events.magistrate-session-ended")
                )));
    }


}
