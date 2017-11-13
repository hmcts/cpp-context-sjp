package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.PersonInfoDetails;

import java.util.UUID;

@Event("sjp.events.person-info-added")
public class PersonInfoAdded {

    private UUID id;
    private UUID caseId;

    @JsonUnwrapped
    private final PersonInfoDetails personInfoDetails;

    public PersonInfoAdded(UUID id, UUID caseId, final PersonInfoDetails personalInfoDetails) {
        this.id = id;
        this.caseId = caseId;
        this.personInfoDetails = personalInfoDetails;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public PersonInfoDetails getPersonInfoDetails() {
        return personInfoDetails;
    }
}
