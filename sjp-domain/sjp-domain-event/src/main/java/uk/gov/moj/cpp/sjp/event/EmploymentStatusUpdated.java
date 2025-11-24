package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.employment-status-updated")
public class EmploymentStatusUpdated {

    private UUID defendantId;

    private String employmentStatus;

    @JsonCreator
    public EmploymentStatusUpdated(@JsonProperty("defendantId") final UUID defendantId,
                                   @JsonProperty("employmentStatus") final String employmentStatus) {
        this.defendantId = defendantId;
        this.employmentStatus = employmentStatus;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }
}