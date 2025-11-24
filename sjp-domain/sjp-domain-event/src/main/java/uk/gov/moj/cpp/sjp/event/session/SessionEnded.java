package uk.gov.moj.cpp.sjp.event.session;

import java.time.ZonedDateTime;
import java.util.UUID;

public abstract class SessionEnded {

    private final UUID sessionId;
    private final ZonedDateTime endedAt;

    public SessionEnded(final UUID sessionId, final ZonedDateTime endedAt) {
        this.sessionId = sessionId;
        this.endedAt = endedAt;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public ZonedDateTime getEndedAt() {
        return endedAt;
    }

}
