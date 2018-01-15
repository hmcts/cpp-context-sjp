package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.magistrate-session-started")
public class MagistrateSessionStarted extends SessionStarted {

    private final String magistrate;

    @JsonCreator
    public MagistrateSessionStarted(
            @JsonProperty("sessionId") UUID sessionId,
            @JsonProperty("legalAdviserId") UUID legalAdviserId,
            @JsonProperty("courtCode") String courtCode,
            @JsonProperty("magistrate") String magistrate,
            @JsonProperty("startedAt") ZonedDateTime startedAt

    ) {
        super(sessionId, legalAdviserId, courtCode, startedAt);
        this.magistrate = magistrate;
    }

    public String getMagistrate() {
        return magistrate;
    }
}
