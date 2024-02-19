package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Address;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-address-update-requested")
public class DefendantAddressUpdateRequested {

    private UUID caseId;
    private Address newAddress;
    private ZonedDateTime updatedAt;

    @JsonCreator
    public DefendantAddressUpdateRequested(@JsonProperty("caseId") UUID caseId,
                                           @JsonProperty("newAddress") Address newAddress,
                                           @JsonProperty("updatedAt") ZonedDateTime updatedAt) {
        this.caseId = caseId;
        this.newAddress = newAddress;
        this.updatedAt = updatedAt;

    }

    public UUID getCaseId() {
        return caseId;
    }

    public Address getNewAddress() { return newAddress; }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }
}
