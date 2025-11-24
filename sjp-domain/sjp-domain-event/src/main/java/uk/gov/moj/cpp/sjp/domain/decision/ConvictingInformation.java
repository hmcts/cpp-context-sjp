package uk.gov.moj.cpp.sjp.domain.decision;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class ConvictingInformation implements Serializable {

    private ZonedDateTime convictionDate;
    private SessionCourt convictingCourt;
    private UUID sessionId;
    private UUID offenceId;

    @JsonCreator
    public ConvictingInformation(@JsonProperty("convictionDate") final ZonedDateTime convictionDate,
                                 @JsonProperty("convictingCourt") final SessionCourt convictingCourt,
                                 @JsonProperty("sessionId") final UUID sessionId,
                                 @JsonProperty("offenceId") final UUID offenceId) {
        this.convictionDate = convictionDate;
        this.convictingCourt = convictingCourt;
        this.sessionId = sessionId;
        this.offenceId = offenceId;
    }

    public ZonedDateTime getConvictionDate() {
        return convictionDate;
    }

    public SessionCourt getConvictingCourt() {
        return convictingCourt;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }
}
