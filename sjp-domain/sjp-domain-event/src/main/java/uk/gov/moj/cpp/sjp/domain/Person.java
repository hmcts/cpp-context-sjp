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
    private final String driverLicenceDetails;
    private final Address address;
    private final ContactDetails contactDetails;
    private final String region;
    private final String legalEntityName;

    @JsonCreator
    public Person(
            @JsonProperty("title") final String title,
            @JsonProperty("firstName") final String firstName,
            @JsonProperty("lastName") final String lastName,
            @JsonProperty("dateOfBirth") final LocalDate dateOfBirth,
            @JsonProperty("gender") final Gender gender,
            @JsonProperty("nationalInsuranceNumber") final String nationalInsuranceNumber,
            @JsonProperty("driverNumber") final String driverNumber,
            @JsonProperty("driverLicenceDetails") final String driverLicenceDetails,
            @JsonProperty("address") final Address address,
            @JsonProperty("contactDetails") final ContactDetails contactDetails,
            @JsonProperty("region") final String region,
            @JsonProperty("legalEntityName") final String legalEntityName) {
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.driverNumber = driverNumber;
        this.driverLicenceDetails = driverLicenceDetails;
        this.address = address;
        this.contactDetails = contactDetails;
        this.region = region;
        this.legalEntityName = legalEntityName;
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

    public String getDriverLicenceDetails() {
        return driverLicenceDetails;
    }

    public Address getAddress() {
        return address;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public String getRegion() {
        return region;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Person that = (Person) o;
        return DomainUtils.equals(title, that.title) &&
                DomainUtils.equals(firstName, that.firstName) &&
                DomainUtils.equals(lastName, that.lastName) &&
                Objects.equals(dateOfBirth, that.dateOfBirth) &&
                Objects.equals(gender, that.gender) &&
                DomainUtils.equals(nationalInsuranceNumber, that.nationalInsuranceNumber) &&
                Objects.equals(driverNumber, that.driverNumber) &&
                Objects.equals(address, that.address) &&
                Objects.equals(contactDetails, that.contactDetails) &&
                DomainUtils.equals(region, that.region) &&
                DomainUtils.equals(legalEntityName, that.legalEntityName);
    }

    @Override
    public int hashCode() {
        return DomainUtils.hash(title, firstName, lastName, dateOfBirth, gender, nationalInsuranceNumber,
                driverNumber, address, contactDetails, region, legalEntityName);
    }

}
