package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.employer-deleted")
public class EmployerDeleted {

    private UUID defendantId;

    @JsonCreator
    public EmployerDeleted(@JsonProperty("defendantId") final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }
}