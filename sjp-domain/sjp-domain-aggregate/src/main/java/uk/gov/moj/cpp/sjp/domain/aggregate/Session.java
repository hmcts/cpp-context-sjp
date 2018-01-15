package uk.gov.moj.cpp.sjp.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.SessionStarted;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class Session implements Aggregate {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID legalAdviserId;
    private String magistrate;
    private String courtCode;
    private ZonedDateTime startedAt;
    private SessionType sessionType;

    public Stream<Object> startSession(final UUID sessionId, final UUID legalAdviserId, final String courtCode, final String magistrate, final ZonedDateTime startedAt) {
        final SessionStarted sessionStarted;

        if (magistrate == null) {
            sessionStarted = new DelegatedPowersSessionStarted(sessionId, legalAdviserId, courtCode, startedAt);
        } else {
            sessionStarted = new MagistrateSessionStarted(sessionId, legalAdviserId, courtCode, magistrate, startedAt);
        }

        return apply(Stream.of(sessionStarted));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(MagistrateSessionStarted.class).apply(magistrateSessionStarted -> {
                    id = magistrateSessionStarted.getSessionId();
                    courtCode = magistrateSessionStarted.getCourtCode();
                    legalAdviserId = magistrateSessionStarted.getLegalAdviserId();
                    startedAt = magistrateSessionStarted.getStartedAt();
                    magistrate = magistrateSessionStarted.getMagistrate();
                    sessionType = MAGISTRATE;
                }),
                when(DelegatedPowersSessionStarted.class).apply(delegatedPowersSessionStarted -> {
                    id = delegatedPowersSessionStarted.getSessionId();
                    courtCode = delegatedPowersSessionStarted.getCourtCode();
                    legalAdviserId = delegatedPowersSessionStarted.getLegalAdviserId();
                    startedAt = delegatedPowersSessionStarted.getStartedAt();
                    sessionType = DELEGATED_POWERS;
                })
        );
    }


    public UUID getId() {
        return id;
    }

    public UUID getUser() {
        return legalAdviserId;
    }

    public Optional<String> getMagistrate() {
        return Optional.ofNullable(magistrate);
    }

    public String getCourtCode() {
        return courtCode;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public SessionType getSessionType() {
        return sessionType;
    }
}
