package uk.gov.moj.cpp.sjp.query.view.response;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

public class SessionView {

    private UUID sessionId;

    private UUID legalAdviserUserId;

    private String courtHouseCode;

    private String courtHouseName;

    private String localJusticeAreaNationalCourtCode;

    private Optional<String> magistrate;

    private ZonedDateTime startedAt;

    private Optional<ZonedDateTime> endedAt;

    private String sessionType;

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getLegalAdviserUserId() {
        return legalAdviserUserId;
    }

    public void setLegalAdviserUserId(UUID legalAdviserUserId) {
        this.legalAdviserUserId = legalAdviserUserId;
    }

    public String getCourtHouseCode() {
        return courtHouseCode;
    }

    public void setCourtHouseCode(String courtHouseCode) {
        this.courtHouseCode = courtHouseCode;
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public void setCourtHouseName(String courtHouseName) {
        this.courtHouseName = courtHouseName;
    }

    public String getLocalJusticeAreaNationalCourtCode() {
        return localJusticeAreaNationalCourtCode;
    }

    public void setLocalJusticeAreaNationalCourtCode(String localJusticeAreaNationalCourtCode) {
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
    }

    public Optional<String> getMagistrate() {
        return magistrate;
    }

    public void setMagistrate(Optional<String> magistrate) {
        this.magistrate = magistrate;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public Optional<ZonedDateTime> getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(final ZonedDateTime endedAt) {
        this.endedAt = Optional.ofNullable(endedAt);
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }
}
