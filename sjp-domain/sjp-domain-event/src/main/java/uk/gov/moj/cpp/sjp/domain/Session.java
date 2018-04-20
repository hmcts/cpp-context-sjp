package uk.gov.moj.cpp.sjp.domain;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Session {

    private final UUID id;
    private final UUID userId;
    private final SessionType type;
    private final String localJusticeAreaNationalCourtCode;

    public Session(
            @JsonProperty("id") final UUID id,
            @JsonProperty("userId") final UUID userId,
            @JsonProperty("type") final SessionType type,
            @JsonProperty("localJusticeAreaNationalCourtCode") final String localJusticeAreaNationalCourtCode) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public SessionType getType() {
        return type;
    }

    public String getLocalJusticeAreaNationalCourtCode() {
        return localJusticeAreaNationalCourtCode;
    }
}
