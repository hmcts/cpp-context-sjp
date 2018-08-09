package uk.gov.moj.cpp.sjp.domain;

import uk.gov.justice.json.schemas.domains.sjp.Gender;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Person {

    private final String title;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final String nationalInsuranceNumber;
    private final String driverNumber;
    private final Address address;
    private final ContactDetails contactDetails;

    @SuppressWarnings("squid:S00107")
    public Person(
            final String title,
            final String firstName,
            final String lastName,
            final LocalDate dateOfBirth,
            final Gender gender,
            final String nationalInsuranceNumber,
            final Address address,
            final ContactDetails contactDetails) {
        this(title, firstName, lastName, dateOfBirth, gender,
                nationalInsuranceNumber, null, address, contactDetails);
    }

    @JsonCreator
    public Person(
            @JsonProperty("title") final String title,
            @JsonProperty("firstName") final String firstName,
            @JsonProperty("lastName") final String lastName,
            @JsonProperty("dateOfBirth") final LocalDate dateOfBirth,
            @JsonProperty("gender") final Gender gender,
            @JsonProperty("nationalInsuranceNumber") final String nationalInsuranceNumber,
            @JsonProperty("driverNumber") final String driverNumber,
            @JsonProperty("address") final Address address,
            @JsonProperty("contactDetails") final ContactDetails contactDetails
    ) {
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.driverNumber = driverNumber;
        this.address = address;
        this.contactDetails = contactDetails;
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

    public Gender getGender() {
        return gender;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public Address getAddress() {
        return address;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Person that = (Person) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(dateOfBirth, that.dateOfBirth) &&
                Objects.equals(gender, that.gender) &&
                Objects.equals(nationalInsuranceNumber, that.nationalInsuranceNumber) &&
                Objects.equals(driverNumber, that.driverNumber) &&
                Objects.equals(address, that.address) &&
                Objects.equals(contactDetails, that.contactDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, firstName, lastName, dateOfBirth, gender, nationalInsuranceNumber,
                driverNumber, address, contactDetails);
    }

}
