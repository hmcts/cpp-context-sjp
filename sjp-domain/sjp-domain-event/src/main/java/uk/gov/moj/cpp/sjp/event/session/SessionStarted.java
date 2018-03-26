package uk.gov.moj.cpp.sjp.event.session;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

public abstract class SessionStarted {

        private final UUID sessionId;
        private final UUID legalAdviserId;
        private final String courtCode;
        private final ZonedDateTime startedAt;

    public SessionStarted(
            final UUID sessionId,
            final UUID legalAdviserId,
            final String courtCode,
            final ZonedDateTime startedAt
    ) {
        this.sessionId = sessionId;
        this.legalAdviserId = legalAdviserId;
        this.courtCode = courtCode;
        this.startedAt = startedAt;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getLegalAdviserId() {
        return legalAdviserId;
    }

    public String getCourtCode() {
        return courtCode;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }
}
