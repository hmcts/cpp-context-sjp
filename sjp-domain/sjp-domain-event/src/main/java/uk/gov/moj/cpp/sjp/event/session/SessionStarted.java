package uk.gov.moj.cpp.sjp.event.session;

import java.time.ZonedDateTime;
import java.util.UUID;

public abstract class SessionStarted {

    private final UUID sessionId;
    private final UUID userId;
    private final String courtHouseCode;
    private final String courtHouseName;
    private final String localJusticeAreaNationalCourtCode;
    private final ZonedDateTime startedAt;

    public SessionStarted(
            final UUID sessionId,
            final UUID userId,
            final String courtHouseCode,
            final String courtHouseName,
            final String localJusticeAreaNationalCourtCode,
            final ZonedDateTime startedAt
    ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.courtHouseCode = courtHouseCode;
        this.courtHouseName = courtHouseName;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
        this.startedAt = startedAt;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
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

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

}
