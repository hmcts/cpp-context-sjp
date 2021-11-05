package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(MagistrateSessionStarted.EVENT_NAME)
public class MagistrateSessionStarted extends SessionStarted {

    public static final String EVENT_NAME = "sjp.events.magistrate-session-started";

    private final String magistrate;

    private final Optional<DelegatedPowers> legalAdviser;

    @JsonCreator
    public MagistrateSessionStarted(
            @JsonProperty("sessionId") UUID sessionId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("courtHouseCode") String courtHouseCode,
            @JsonProperty("courtHouseName") String courtHouseName,
            @JsonProperty("localJusticeAreaNationalCourtCode") String localJusticeAreaNationalCourtCode,
            @JsonProperty("startedAt") ZonedDateTime startedAt,
            @JsonProperty("magistrate") String magistrate,
            @JsonProperty("legalAdviser") Optional<DelegatedPowers> legalAdviser
    ) {
        super(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt);
        this.magistrate = magistrate;
        this.legalAdviser = legalAdviser;
    }

    public String getMagistrate() {
        return magistrate;
    }

    public Optional<DelegatedPowers> getLegalAdviser() {
        return legalAdviser;
    }
}
