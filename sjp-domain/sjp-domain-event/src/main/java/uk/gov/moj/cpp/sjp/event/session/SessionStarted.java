package uk.gov.moj.cpp.sjp.event.session;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public abstract class SessionStarted {

    private final UUID sessionId;
    private final UUID userId;
    private final String courtHouseCode;
    private final String courtHouseName;
    private final String localJusticeAreaNationalCourtCode;
    private final ZonedDateTime startedAt;
    private final List<String> prosecutors;

    public SessionStarted(
            final UUID sessionId,
            final UUID userId,
            final String courtHouseCode,
            final String courtHouseName,
            final String localJusticeAreaNationalCourtCode,
            final ZonedDateTime startedAt,
            final List<String> prosecutors
    ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.courtHouseCode = courtHouseCode;
        this.courtHouseName = courtHouseName;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
        this.startedAt = startedAt;
        this.prosecutors = prosecutors;
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

    public List<String> getProsecutors() {
        return prosecutors;
    }
}
