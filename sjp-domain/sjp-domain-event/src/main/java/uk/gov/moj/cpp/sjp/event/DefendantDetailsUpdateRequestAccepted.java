package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.PersonalName;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-details-update-request-accepted")
public class DefendantDetailsUpdateRequestAccepted {

    private final UUID caseId;
    private final UUID defendantId;
    private final PersonalName newPersonalName;
    private final String newLegalEntityName;
    private final Address newAddress;
    private final LocalDate newDateOfBirth;
    private final ZonedDateTime updatedAt;

    @JsonCreator
    public DefendantDetailsUpdateRequestAccepted(@JsonProperty("caseId") UUID caseId,
                                                 @JsonProperty("defendantId") UUID defendantId,
                                                 @JsonProperty("newPersonalName") PersonalName newPersonalName,
                                                 @JsonProperty("newLegalEntityName") String newLegalEntityName,
                                                 @JsonProperty("newAddress") Address newAddress,
                                                 @JsonProperty("newDateOfBirth") LocalDate newDateOfBirth,
                                                 @JsonProperty("updatedAt") ZonedDateTime updatedAt) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.newPersonalName = newPersonalName;
        this.newLegalEntityName = newLegalEntityName;
        this.newAddress = newAddress;
        this.newDateOfBirth = newDateOfBirth;
        this.updatedAt = updatedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public PersonalName getNewPersonalName() {
        return newPersonalName;
    }

    public String getNewLegalEntityName() {
        return newLegalEntityName;
    }

    public Address getNewAddress() {
        return newAddress;
    }

    public LocalDate getNewDateOfBirth() {
        return newDateOfBirth;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }
}

