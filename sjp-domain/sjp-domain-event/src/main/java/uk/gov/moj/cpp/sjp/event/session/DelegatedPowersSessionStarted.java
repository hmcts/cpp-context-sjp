package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.delegated-powers-session-started")
public class DelegatedPowersSessionStarted extends SessionStarted {

    @JsonCreator
    public DelegatedPowersSessionStarted(
            @JsonProperty("sessionId") UUID sessionId,
            @JsonProperty("legalAdviserId") UUID legalAdviserId,
            @JsonProperty("courtCode") String courtCode,
            @JsonProperty("startedAt") ZonedDateTime startedAt

    ) {
        super(sessionId, legalAdviserId, courtCode, startedAt);
    }

}
