package uk.gov.moj.cpp.sjp.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {

    private final UUID id;
    private final UUID userId;
    private final SessionType type;
    private final String courtHouseCode;
    private final String localJusticeAreaNationalCourtCode;
    private final List<String> prosecutors;

    public Session(
            @JsonProperty("id") final UUID id,
            @JsonProperty("userId") final UUID userId,
            @JsonProperty("type") final SessionType type,
            @JsonProperty("courtHouseCode") String courtHouseCode,
            @JsonProperty("localJusticeAreaNationalCourtCode") String localJusticeAreaNationalCourtCode,
            @JsonProperty("prosecutors") List<String> prosecutors) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.courtHouseCode = courtHouseCode;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
        this.prosecutors = prosecutors;
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

    public String getCourtHouseCode() {
        return courtHouseCode;
    }

    public String getLocalJusticeAreaNationalCourtCode() {
        return localJusticeAreaNationalCourtCode;
    }

    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Session session = (Session) o;
        return Objects.equals(id, session.id) &&
                Objects.equals(userId, session.userId) &&
                type == session.type &&
                Objects.equals(courtHouseCode, session.courtHouseCode) &&
                Objects.equals(localJusticeAreaNationalCourtCode, session.localJusticeAreaNationalCourtCode) &&
                Objects.equals(prosecutors, session.prosecutors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, type, courtHouseCode, localJusticeAreaNationalCourtCode, prosecutors);
    }

    public List<String> getProsecutors() {
        return prosecutors;
    }
}
