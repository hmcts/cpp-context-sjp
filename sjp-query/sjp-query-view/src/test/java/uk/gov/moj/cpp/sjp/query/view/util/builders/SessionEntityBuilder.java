package uk.gov.moj.cpp.sjp.query.view.util.builders;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.time.ZonedDateTime;
import java.util.UUID;

public class SessionEntityBuilder {

    private UUID userId;
    private UUID sessionId;
    private String courtHouseCode;
    private String courtHouseName;
    private String localJusticeAreaNationalCourtCode;
    private String magistrate;
    private ZonedDateTime startedAt;

    private SessionEntityBuilder(final UUID sessionId,
                                 final UUID userId,
                                 final String courtHouseCode,
                                 final String courtHouseName,
                                 final String localJusticeAreaNationalCourtCode,
                                 final String magistrate,
                                 final ZonedDateTime startedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.courtHouseCode = courtHouseCode;
        this.courtHouseName = courtHouseName;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
        this.magistrate = magistrate;
        this.startedAt = startedAt;
    }

    public static SessionEntityBuilder withDefaults() {
        return new SessionEntityBuilder(
                randomUUID(),
                randomUUID(),
                "ASDF",
                "Lavender Hill",
                "YUIO",
                "Legal adviser name",
                now()
        );
    }

    public Session build() {
        return new Session(
                sessionId,
                userId,
                courtHouseCode,
                courtHouseName,
                localJusticeAreaNationalCourtCode,
                magistrate,
                startedAt
        );
    }

    public SessionEntityBuilder withMagistrate(final String magistrate) {
        this.magistrate = magistrate;
        return this;
    }
}
