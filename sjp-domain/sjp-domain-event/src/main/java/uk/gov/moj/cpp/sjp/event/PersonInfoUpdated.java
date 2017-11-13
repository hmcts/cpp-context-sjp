package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.PersonInfoDetails;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

@Event("sjp.events.person-info-updated")
public class PersonInfoUpdated {

    @JsonUnwrapped
    private PersonInfoDetails personInfoDetails;

    public PersonInfoUpdated(final PersonInfoDetails personInfoDetails) {
        this.personInfoDetails = personInfoDetails;
    }

    public PersonInfoDetails getPersonInfoDetails() {
        return personInfoDetails;
    }
}
