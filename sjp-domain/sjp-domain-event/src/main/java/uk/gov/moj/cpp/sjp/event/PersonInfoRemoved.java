package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("sjp.events.person-info-removed")
public class PersonInfoRemoved {

    private UUID personInfoId;
    private UUID caseId;
    private UUID personId;

    public PersonInfoRemoved(final UUID personInfoId,
                             final UUID caseId,
                             final UUID personId) {
        this.personInfoId = personInfoId;
        this.caseId = caseId;
        this.personId = personId;
    }

    public UUID getPersonInfoId() {
        return personInfoId;
    }

    public void setPersonInfoId(UUID id) {
        this.personInfoId = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }


}
