package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.PersonalName;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-personal-name-updated")
public class DefendantPersonalNameUpdated {

    private UUID caseId;
    private PersonalName oldPersonalName;
    private PersonalName newPersonalName;

    @JsonCreator
    public DefendantPersonalNameUpdated(@JsonProperty("caseId") UUID caseId,
                                        @JsonProperty("oldPersonalName") PersonalName oldPersonalName,
                                        @JsonProperty("newPersonalName") PersonalName newPersonalName) {
        this.caseId = caseId;
        this.oldPersonalName = oldPersonalName;
        this.newPersonalName = newPersonalName;
    }

    public UUID getCaseId() { return caseId; }

    public PersonalName getOldPersonalName() { return oldPersonalName; }

    public PersonalName getNewPersonalName() { return newPersonalName; }

}
