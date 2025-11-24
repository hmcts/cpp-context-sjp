package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.PersonalName;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-name-update-requested")
public class DefendantNameUpdateRequested {

    private final UUID caseId;
    private final PersonalName newPersonalName;
    private final String newLegalEntityName;
    private final ZonedDateTime updatedAt;


    @JsonCreator
    public DefendantNameUpdateRequested(@JsonProperty("caseId") UUID caseId,
                                        @JsonProperty("newPersonalName") PersonalName newPersonalName,
                                        @JsonProperty("newLegalEntityName") String newLegalEntityName,
                                        @JsonProperty("updatedAt") ZonedDateTime updatedAt) {
        this.caseId = caseId;
        this.newPersonalName = newPersonalName;
        this.newLegalEntityName = newLegalEntityName;
        this.updatedAt = updatedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public PersonalName getNewPersonalName() {
        return newPersonalName;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getNewLegalEntityName() {
        return newLegalEntityName;
    }
}
