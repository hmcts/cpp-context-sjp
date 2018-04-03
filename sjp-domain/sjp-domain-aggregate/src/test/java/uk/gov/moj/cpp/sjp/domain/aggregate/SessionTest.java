package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.SessionEnded;
import uk.gov.moj.cpp.sjp.event.session.SessionStarted;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class SessionTest {

    private UUID sessionId, userId;
    private String courtHouseName, localJusticeAreaNationalCourtCode;
    private ZonedDateTime startedAt, endedAt;
    private Session session;

    @Before
    public void init() {
        sessionId = randomUUID();
        userId = randomUUID();
        localJusticeAreaNationalCourtCode = "2924";
        courtHouseName = "Coventry Magistrates' Court";
        startedAt = ZonedDateTime.now();
        endedAt = startedAt.plusMinutes(1);
        session = new Session();
    }

    @Test
    public void shouldStartAndEndDelegatedPowersSession() {
        final SessionStarted expectedSessionStartedEvent = new DelegatedPowersSessionStarted(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
        final SessionEnded expectedSessionEndedEvent = new DelegatedPowersSessionEnded(sessionId, endedAt);

        final Stream<Object> startSessionEvents = session.startDelegatedPowersSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
        assertThat(startSessionEvents.collect(toList()), containsInAnyOrder(expectedSessionStartedEvent));

        final Stream<Object> endSessionEvents = session.endSession(sessionId, endedAt);
        assertThat(endSessionEvents.collect(toList()), containsInAnyOrder(expectedSessionEndedEvent));
    }

    @Test
    public void shouldStartAndEndMagistrateSession() {
        final String magistrate = "magistrate";

        final SessionStarted expectedSessionStartedEvent = new MagistrateSessionStarted(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate);
        final SessionEnded expectedSessionEndedEvent = new MagistrateSessionEnded(sessionId, endedAt);

        final Stream<Object> startSessionEvents = session.startMagistrateSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate);
        assertThat(startSessionEvents.collect(toList()), containsInAnyOrder(expectedSessionStartedEvent));

        final Stream<Object> endSessionEvents = session.endSession(sessionId, endedAt);
        assertThat(endSessionEvents.collect(toList()), containsInAnyOrder(expectedSessionEndedEvent));
    }

    @Test
    public void shouldNotStartAlreadyStartedSession() {
        final Stream<Object> startSessionEvents1 = session.startDelegatedPowersSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
        final Stream<Object> startSessionEvents2 = session.startDelegatedPowersSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt.plusMinutes(1));
        final Stream<Object> startSessionEvents3 = session.startMagistrateSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt.plusMinutes(1), "Alan");

        assertThat(startSessionEvents1.collect(toList()), hasSize(1));
        assertThat(startSessionEvents2.collect(toList()), hasSize(0));
        assertThat(startSessionEvents3.collect(toList()), hasSize(0));
    }

    @Test
    public void shouldNotEndAlreadyEndedSession() {
        session.startDelegatedPowersSession(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
        session.endSession(sessionId, endedAt);

        assertThat(session.endSession(sessionId, endedAt).collect(toList()), hasSize(0));
    }

    @Test
    public void shouldNotEndNotStartedSession() {
        assertThat(session.endSession(sessionId, endedAt).collect(toList()), hasSize(0));
    }

}
