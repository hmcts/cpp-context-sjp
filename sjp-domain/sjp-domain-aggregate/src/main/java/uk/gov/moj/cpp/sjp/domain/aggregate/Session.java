package uk.gov.moj.cpp.sjp.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRequested;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.SessionStarted;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Session implements Aggregate {

    private static final long serialVersionUID = 5L;
    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);

    private UUID id;
    private UUID userId;
    private String courtHouseCode;
    private String courtHouseName;
    private String localJusticeAreaNationalCourtCode;
    private SessionType sessionType;
    private String magistrate;
    private SessionState sessionState = SessionState.NOT_EXISTING;

    public Stream<Object> startDelegatedPowersSession(
            final UUID sessionId,
            final UUID userId,
            final String courtHouseCode,
            final String courtHouseName,
            final String localJusticeAreaNationalCourtCode,
            final ZonedDateTime startedAt) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (sessionState == SessionState.NOT_EXISTING) {
            streamBuilder.add(new DelegatedPowersSessionStarted(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt));
        } else {
            LOGGER.warn("Delegated powers session can not be started - session {} already exists", sessionId);
        }

        return apply(streamBuilder.build());
    }

    public Stream<Object> startMagistrateSession(
            final UUID sessionId,
            final UUID userId,
            final String courtHouseCode,
            final String courtHouseName,
            final String localJusticeAreaNationalCourtCode,
            final ZonedDateTime startedAt,
            final String magistrate,
            final Optional<DelegatedPowers> legalAdviserOpt) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (sessionState == SessionState.NOT_EXISTING) {
            streamBuilder.add(new MagistrateSessionStarted(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate, legalAdviserOpt));
        } else {
            LOGGER.warn("Magistrate session can not be started - session {} already exists", sessionId);
        }

        return apply(streamBuilder.build());
    }

    public Stream<Object> endSession(final UUID sessionId, final ZonedDateTime endedAt) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (sessionState == SessionState.STARTED) {
            if (MAGISTRATE.equals(sessionType)) {
                streamBuilder.add(new MagistrateSessionEnded(sessionId, endedAt));
            } else if (DELEGATED_POWERS.equals(sessionType)) {
                streamBuilder.add(new DelegatedPowersSessionEnded(sessionId, endedAt));
            }
        } else {
            LOGGER.warn("Session can not be ended - session {} is not started", sessionId);
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> requestCaseAssignment(final UUID userId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (sessionState == SessionState.NOT_EXISTING) {
            streamBuilder.add(new CaseAssignmentRejected(CaseAssignmentRejected.RejectReason.SESSION_DOES_NOT_EXIST));
        } else if (sessionState == SessionState.ENDED) {
            streamBuilder.add(new CaseAssignmentRejected(CaseAssignmentRejected.RejectReason.SESSION_ENDED));
        } else if (!userId.equals(this.userId)) {
            streamBuilder.add(new CaseAssignmentRejected(CaseAssignmentRejected.RejectReason.SESSION_NOT_OWNED_BY_USER));
        } else {
            streamBuilder.add(new CaseAssignmentRequested(new uk.gov.moj.cpp.sjp.domain.Session(id, userId, sessionType, courtHouseCode, localJusticeAreaNationalCourtCode)));
        }

        return apply(streamBuilder.build());
    }

    public Stream<Object> rejectCaseAssignment(final CaseAssignmentRejected.RejectReason reason) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (sessionState == SessionState.ENDED) {
            streamBuilder.add(new CaseAssignmentRejected(CaseAssignmentRejected.RejectReason.SESSION_ENDED));
        } else {
            streamBuilder.add(new CaseAssignmentRejected(reason));
        }

        return apply(streamBuilder.build());
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(MagistrateSessionStarted.class).apply(sessionStarted -> {
                    applySessionStartedEvent(sessionStarted);
                    magistrate = sessionStarted.getMagistrate();
                    sessionType = MAGISTRATE;
                }),
                when(DelegatedPowersSessionStarted.class).apply(sessionStarted -> {
                    applySessionStartedEvent(sessionStarted);
                    magistrate = null;
                    sessionType = DELEGATED_POWERS;
                }),
                when(MagistrateSessionEnded.class).apply(sessionEnded -> sessionState = SessionState.ENDED),
                when(DelegatedPowersSessionEnded.class).apply(sessionEnded -> sessionState = SessionState.ENDED),
                otherwiseDoNothing()
        );
    }

    private void applySessionStartedEvent(final SessionStarted sessionStarted) {
        id = sessionStarted.getSessionId();
        courtHouseCode = sessionStarted.getCourtHouseCode();
        courtHouseName = sessionStarted.getCourtHouseName();
        localJusticeAreaNationalCourtCode = sessionStarted.getLocalJusticeAreaNationalCourtCode();
        userId = sessionStarted.getUserId();
        sessionState = SessionState.STARTED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUser() {
        return userId;
    }

    public Optional<String> getMagistrate() {
        return Optional.ofNullable(magistrate);
    }

    public String getCourtHouseCode() {
        return courtHouseCode;
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public String getLocalJusticeAreaNationalCourtCode() {
        return localJusticeAreaNationalCourtCode;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setCourtHouseCode(String courtHouseCode) {
        this.courtHouseCode = courtHouseCode;
    }

    // Expose to test
    public void setLocalJusticeAreaNationalCourtCode(String localJusticeAreaNationalCourtCode) {
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
    }

    private enum SessionState {
        NOT_EXISTING, STARTED, ENDED
    }

}
