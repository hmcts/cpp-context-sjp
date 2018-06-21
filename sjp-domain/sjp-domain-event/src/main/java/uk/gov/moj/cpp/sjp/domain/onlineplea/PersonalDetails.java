package uk.gov.moj.cpp.sjp.domain.onlineplea;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;

import java.time.LocalDate;

public class PersonalDetails {
    private final String firstName;
    private final String lastName;
    private final Address address;
    private final ContactDetails contactDetails;
    private final LocalDate dateOfBirth;
    private final String nationalInsuranceNumber;

    public PersonalDetails(final String firstName, final String lastName, final Address address, final ContactDetails contactDetails,
                           final LocalDate dateOfBirth, final String nationalInsuranceNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.contactDetails = contactDetails;
        this.dateOfBirth = dateOfBirth;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Address getAddress() {
        return address;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }
}