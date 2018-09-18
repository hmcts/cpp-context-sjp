package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.PersonalName;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-personal-name-updated")
public class DefendantPersonalNameUpdated {

    private UUID caseId;
    private PersonalName oldPersonalName;
    private PersonalName newPersonalName;
    private ZonedDateTime updatedAt;

    @JsonCreator
    public DefendantPersonalNameUpdated(@JsonProperty("caseId") UUID caseId,
                                        @JsonProperty("oldPersonalName") PersonalName oldPersonalName,
                                        @JsonProperty("newPersonalName") PersonalName newPersonalName,
                                        @JsonProperty("updatedAt") ZonedDateTime updatedAt) {
        this.caseId = caseId;
        this.oldPersonalName = oldPersonalName;
        this.newPersonalName = newPersonalName;
        this.updatedAt = updatedAt;
    }

    public UUID getCaseId() { return caseId; }

    public PersonalName getOldPersonalName() { return oldPersonalName; }

    public PersonalName getNewPersonalName() { return newPersonalName; }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }
}
