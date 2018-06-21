package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(MagistrateSessionStarted.EVENT_NAME)
public class MagistrateSessionStarted extends SessionStarted {

    public static final String EVENT_NAME = "sjp.events.magistrate-session-started";

    private final String magistrate;

    @JsonCreator
    public MagistrateSessionStarted(
            @JsonProperty("sessionId") UUID sessionId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("courtHouseName") String courtHouseName,
            @JsonProperty("localJusticeAreaNationalCourtCode") String localJusticeAreaNationalCourtCode,
            @JsonProperty("startedAt") ZonedDateTime startedAt,
            @JsonProperty("magistrate") String magistrate
    ) {
        super(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
        this.magistrate = magistrate;
    }

    public String getMagistrate() {
        return magistrate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final MagistrateSessionStarted that = (MagistrateSessionStarted) o;
        return Objects.equals(magistrate, that.magistrate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), magistrate);
    }
}
