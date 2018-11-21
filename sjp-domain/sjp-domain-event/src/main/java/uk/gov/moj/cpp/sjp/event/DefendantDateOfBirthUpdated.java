package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-date-of-birth-updated")
public class DefendantDateOfBirthUpdated {

    private UUID caseId;
    private LocalDate oldDateOfBirth;
    private LocalDate newDateOfBirth;
    private ZonedDateTime updatedAt;

    @JsonCreator
    public DefendantDateOfBirthUpdated(@JsonProperty("caseId") UUID caseId,
                                       @JsonProperty("oldDateOfBirth") LocalDate oldDateOfBirth,
                                       @JsonProperty("newDateOfBirth") LocalDate newDateOfBirth,
                                       @JsonProperty("updatedAt") ZonedDateTime updatedAt) {
        this.caseId = caseId;
        this.oldDateOfBirth = oldDateOfBirth;
        this.newDateOfBirth = newDateOfBirth;
        this.updatedAt = updatedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public LocalDate getOldDateOfBirth() {
        return oldDateOfBirth;
    }

    public LocalDate getNewDateOfBirth() {
        return newDateOfBirth;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

}
