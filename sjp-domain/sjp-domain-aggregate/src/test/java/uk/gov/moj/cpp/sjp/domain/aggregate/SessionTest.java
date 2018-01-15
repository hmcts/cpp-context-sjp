package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class SessionTest {

    private UUID sessionId, legalAdviserId;
    private String courtCode;
    private ZonedDateTime sessionStartedAt;

    private Session session;

    @Before
    public void init() {
        sessionId = UUID.randomUUID();
        legalAdviserId = UUID.randomUUID();
        courtCode = "5000";
        sessionStartedAt = ZonedDateTime.now();
        session = new Session();
    }

    @Test
    public void shouldCreateDelegatedPowersSessionStartedEvent() {
        final Stream<Object> sessionStartedEventStream = session.startSession(sessionId, legalAdviserId, courtCode, null, sessionStartedAt);
        final List<Object> sessionStartedEvents = sessionStartedEventStream.collect(Collectors.toList());

        assertThat(sessionStartedEvents, hasSize(1));

        assertThat(sessionStartedEvents.get(0), instanceOf(DelegatedPowersSessionStarted.class));

        final DelegatedPowersSessionStarted sessionStartedEvent = (DelegatedPowersSessionStarted) sessionStartedEvents.get(0);
        assertThat(sessionStartedEvent.getSessionId(), equalTo(sessionId));
        assertThat(sessionStartedEvent.getLegalAdviserId(), equalTo(legalAdviserId));
        assertThat(sessionStartedEvent.getCourtCode(), equalTo(courtCode));
        assertThat(sessionStartedEvent.getStartedAt(), equalTo(sessionStartedAt));
    }

    @Test
    public void shouldCreateMagistrateSessionStartedEvent() {
        final String magistrate = "magistrate";
        final Stream<Object> sessionStartedEventStream = session.startSession(sessionId, legalAdviserId, courtCode, magistrate, sessionStartedAt);
        final List<Object> sessionStartedEvents = sessionStartedEventStream.collect(Collectors.toList());

        assertThat(sessionStartedEvents, hasSize(1));

        assertThat(sessionStartedEvents.get(0), instanceOf(MagistrateSessionStarted.class));

        final MagistrateSessionStarted sessionStartedEvent = (MagistrateSessionStarted) sessionStartedEvents.get(0);
        assertThat(sessionStartedEvent.getSessionId(), equalTo(sessionId));
        assertThat(sessionStartedEvent.getLegalAdviserId(), equalTo(legalAdviserId));
        assertThat(sessionStartedEvent.getCourtCode(), equalTo(courtCode));
        assertThat(sessionStartedEvent.getStartedAt(), equalTo(sessionStartedAt));
        assertThat(sessionStartedEvent.getMagistrate(), equalTo(magistrate));
    }
}
