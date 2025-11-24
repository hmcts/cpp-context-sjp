package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-detail-update-requested")
public class DefendantDetailUpdateRequested {

    private final UUID caseId;
    private final Boolean nameUpdated;
    private final Boolean addressUpdated;
    private final Boolean dobUpdated;

    @JsonCreator
    public DefendantDetailUpdateRequested(@JsonProperty("caseId") UUID caseId,
                                          @JsonProperty("nameUpdated") Boolean nameUpdated,
                                          @JsonProperty("addressUpdated") Boolean addressUpdated,
                                          @JsonProperty("dobUpdated") Boolean dobUpdated) {
        this.caseId = caseId;
        this.nameUpdated = nameUpdated;
        this.addressUpdated = addressUpdated;
        this.dobUpdated = dobUpdated;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Boolean getNameUpdated() {
        return nameUpdated;
    }

    public Boolean getAddressUpdated() {
        return addressUpdated;
    }

    public Boolean getDobUpdated() {
        return dobUpdated;
    }
}
