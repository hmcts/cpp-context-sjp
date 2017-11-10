package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class PersonInfoDetails implements Serializable {

    private final UUID personId;
    private final String title;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    @JsonUnwrapped
    private final Address address;

    public PersonInfoDetails(final UUID personId, final String title, final String firstName, final String lastName, final LocalDate dateOfBirth,
                             final Address address) {
        this.personId = personId;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Address getAddress() {
        return address;
    }
}
