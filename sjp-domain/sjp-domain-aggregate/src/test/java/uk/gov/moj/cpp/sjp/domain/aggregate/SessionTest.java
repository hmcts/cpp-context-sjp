package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class SessionTest extends CaseAggregateBaseTest {

    private UUID sessionId, userId;
    private String courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode;
    private ZonedDateTime startedAt, endedAt;
    private Session session;

    @Before
    public void init() {
        sessionId = randomUUID();
        userId = randomUUID();
        courtHouseCode = "B01LY";
        localJusticeAreaNationalCourtCode = "2924";
        courtHouseName = "Coventry Magistrates' Court";
        startedAt = clock.now();
        endedAt = startedAt.plusMinutes(1);
        session = new Session();
    }

    @Test
    public void shouldStartAndEndDelegatedPowersSession() {
        when(session.startDelegatedPowersSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt))
                .thenExpect(new DelegatedPowersSessionStarted(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt));

        when(session.endSession(sessionId, endedAt))
                .thenExpect(new DelegatedPowersSessionEnded(sessionId, endedAt));
    }

    @Test
    public void shouldStartAndEndMagistrateSession() {
        final String magistrate = "magistrate";

        when(session.startMagistrateSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate))
                .thenExpect(new MagistrateSessionStarted(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate));

        when(session.endSession(sessionId, endedAt))
                .thenExpect(new MagistrateSessionEnded(sessionId, endedAt));
    }

    @Test
    public void shouldNotStartAlreadyStartedSession() {
        final Stream<Object> startSessionEvents1 = session.startDelegatedPowersSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
        final Stream<Object> startSessionEvents2 = session.startDelegatedPowersSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt.plusMinutes(1));
        final Stream<Object> startSessionEvents3 = session.startMagistrateSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt.plusMinutes(1), "Alan");

        assertThat(startSessionEvents1.collect(toList()), hasSize(1));
        assertThat(startSessionEvents2.collect(toList()), hasSize(0));
        assertThat(startSessionEvents3.collect(toList()), hasSize(0));
    }

    @Test
    public void shouldNotEndAlreadyEndedSession() {
        when(session.startDelegatedPowersSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt))
                .reason("no any events emitted")
                .thenExpect();

        when(session.endSession(sessionId, endedAt))
                .reason("no any events emitted")
                .thenExpect();
    }

    @Test
    public void shouldNotEndNotStartedSession() {
        when(session.endSession(sessionId, endedAt))
                .reason("no any events emitted")
                .thenExpect();
    }

}
