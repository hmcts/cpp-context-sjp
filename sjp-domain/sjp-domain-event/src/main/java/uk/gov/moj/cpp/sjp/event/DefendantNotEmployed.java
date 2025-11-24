package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-not-employed")
public class DefendantNotEmployed {

    private UUID defendantId;

    @JsonCreator
    public DefendantNotEmployed(@JsonProperty("defendantId") final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }
}