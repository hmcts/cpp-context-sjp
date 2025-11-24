package uk.gov.moj.cpp.sjp.domain.resulting;

import uk.gov.moj.cpp.sjp.domain.SessionType;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SJPSession {

    private final UUID id;
    private final UUID userId;
    private final SessionType type;
    private final CourtDetails courtDetails;
    private final String magistrate;
    private final ZonedDateTime startedAt;
    private final ZonedDateTime endedAt;

    public SJPSession(
            @JsonProperty("id") final UUID id,
            @JsonProperty("userId") final UUID userId,
            @JsonProperty("type") final SessionType type,
            @JsonProperty("courtDetails") final CourtDetails courtDetails,
            @JsonProperty("magistrate") final String magistrate,
            @JsonProperty("startedAt") final ZonedDateTime startedAt,
            @JsonProperty("endedAt") final ZonedDateTime endedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.courtDetails = courtDetails;
        this.magistrate = magistrate;
        this.startedAt = startedAt;
        this.endedAt = endedAt;

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

    public CourtDetails getCourtDetails() {
        return courtDetails;
    }

    public String getMagistrate() {
        return magistrate;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public ZonedDateTime getEndedAt() {
        return endedAt;
    }

}

