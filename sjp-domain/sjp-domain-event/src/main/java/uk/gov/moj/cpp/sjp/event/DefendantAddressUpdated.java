package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Address;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-address-updated")
public class DefendantAddressUpdated {

    private UUID caseId;
    private Address oldAddress;
    private Address newAddress;

    @JsonCreator
    public DefendantAddressUpdated(@JsonProperty("caseId") UUID caseId,
                                   @JsonProperty("oldAddress") Address oldAddress,
                                   @JsonProperty("newAddress") Address newAddress) {
        this.caseId = caseId;
        this.oldAddress = oldAddress;
        this.newAddress = newAddress;

    }

    public UUID getCaseId() {
        return caseId;
    }

    public Address getOldAddress() { return oldAddress; }

    public Address getNewAddress() { return newAddress; }


}
