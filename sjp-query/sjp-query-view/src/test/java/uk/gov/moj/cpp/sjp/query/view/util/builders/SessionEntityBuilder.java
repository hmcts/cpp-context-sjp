package uk.gov.moj.cpp.sjp.query.view.util.builders;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;

import java.util.List;
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
    private List<String> prosecutors;

    private SessionEntityBuilder(final UUID sessionId,
                                 final UUID userId,
                                 final String courtHouseCode,
                                 final String courtHouseName,
                                 final String localJusticeAreaNationalCourtCode,
                                 final String magistrate,
                                 final ZonedDateTime startedAt,
                                 final List<String> prosecutors) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.courtHouseCode = courtHouseCode;
        this.courtHouseName = courtHouseName;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
        this.magistrate = magistrate;
        this.startedAt = startedAt;
        this.prosecutors = prosecutors;
    }

    public static SessionEntityBuilder withDefaults() {
        return new SessionEntityBuilder(
                randomUUID(),
                randomUUID(),
                "ASDF",
                "Lavender Hill",
                "YUIO",
                "Legal adviser name",
                now(),
                null
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
                startedAt,
                prosecutors
        );
    }

    public SessionEntityBuilder withMagistrate(final String magistrate) {
        this.magistrate = magistrate;
        return this;
    }
}
