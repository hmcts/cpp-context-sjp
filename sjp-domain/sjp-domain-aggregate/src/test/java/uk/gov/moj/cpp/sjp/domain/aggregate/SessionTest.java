package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class SessionTest {

    private final Clock clock = new StoppedClock(new UtcClock().now());
    private UUID sessionId, userId;
    private String courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode;
    private ZonedDateTime startedAt, endedAt;
    private Session session;
    private Optional<DelegatedPowers> legalAdviser;

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
        legalAdviser = Optional.of(DelegatedPowers.delegatedPowers().withFirstName("Erica").withLastName("Wilson").withUserId(randomUUID()).build());
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

        when(session.startMagistrateSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate, legalAdviser))
                .thenExpect(new MagistrateSessionStarted(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate, legalAdviser));

        when(session.endSession(sessionId, endedAt))
                .thenExpect(new MagistrateSessionEnded(sessionId, endedAt));
    }

    @Test
    public void shouldNotStartAlreadyStartedSession() {
        final Stream<Object> startSessionEvents1 = session.startDelegatedPowersSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
        final Stream<Object> startSessionEvents2 = session.startDelegatedPowersSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt.plusMinutes(1));
        final Stream<Object> startSessionEvents3 = session.startMagistrateSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt.plusMinutes(1), "Alan", legalAdviser);

        assertThat(startSessionEvents1.collect(toList()), hasSize(1));
        assertThat(startSessionEvents2.collect(toList()), hasSize(0));
        assertThat(startSessionEvents3.collect(toList()), hasSize(0));
    }

    @Test
    public void shouldNotEndAlreadyEndedSession() {
        session.startDelegatedPowersSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
        session.endSession(sessionId, endedAt);

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
