package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(DelegatedPowersSessionStarted.EVENT_NAME)
public class DelegatedPowersSessionStarted extends SessionStarted {

    public static final String EVENT_NAME = "sjp.events.delegated-powers-session-started";

    @JsonCreator
    public DelegatedPowersSessionStarted(
            @JsonProperty("sessionId") UUID sessionId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("courtHouseCode") String courtHouseCode,
            @JsonProperty("courtHouseName") String courtHouseName,
            @JsonProperty("localJusticeAreaNationalCourtCode") String localJusticeAreaNationalCourtCode,
            @JsonProperty("startedAt") ZonedDateTime startedAt

    ) {
        super(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
    }

}
