package uk.gov.moj.cpp.sjp.event.session;

import java.time.ZonedDateTime;
import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SessionEnded that = (SessionEnded) o;
        return Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(endedAt, that.endedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, endedAt);
    }
}
