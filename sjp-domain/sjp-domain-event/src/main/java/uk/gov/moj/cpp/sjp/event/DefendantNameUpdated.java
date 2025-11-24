package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.PersonalName;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-name-updated")
public class DefendantNameUpdated {

    private final UUID caseId;
    private final PersonalName oldPersonalName;
    private final PersonalName newPersonalName;
    private final String oldLegalEntityName;
    private final String newLegalEntityName;
    private final ZonedDateTime updatedAt;


    @JsonCreator
    public DefendantNameUpdated(@JsonProperty("caseId") UUID caseId,
                                @JsonProperty("oldPersonalName") PersonalName oldPersonalName,
                                @JsonProperty("newPersonalName") PersonalName newPersonalName,
                                @JsonProperty("oldLegalEntityName") String oldLegalEntityName,
                                @JsonProperty("newLegalEntityName") String newLegalEntityName,
                                @JsonProperty("updatedAt") ZonedDateTime updatedAt) {
        this.caseId = caseId;
        this.oldPersonalName = oldPersonalName;
        this.newPersonalName = newPersonalName;
        this.oldLegalEntityName = oldLegalEntityName;
        this.newLegalEntityName = newLegalEntityName;
        this.updatedAt = updatedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public PersonalName getOldPersonalName() {
        return oldPersonalName;
    }

    public PersonalName getNewPersonalName() {
        return newPersonalName;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getOldLegalEntityName() {
        return oldLegalEntityName;
    }

    public String getNewLegalEntityName() {
        return newLegalEntityName;
    }
}
