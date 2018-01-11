package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public class PersonInfoDetails extends Person implements Serializable {

    private static final long serialVersionUID = 1159139108133701584L;

    private final UUID personId;

    public PersonInfoDetails(UUID personId, String title, String firstName, String lastName, LocalDate dateOfBirth, Address address) {
        super(title, firstName, lastName, dateOfBirth, null, address);
        this.personId = personId;
    }

    public UUID getPersonId() {
        return personId;
    }
}
