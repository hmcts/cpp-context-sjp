package uk.gov.moj.cpp.sjp.persistence.builder;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;

public class PersonalDetailsBuilder {

    private String title;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String nationalInsuranceNumber;
    private String driverNumber;
    private String driverLicenceDetails;

    public PersonalDetailsBuilder() {
    }

    public static PersonalDetailsBuilder buildPersonalDetails() {
        return new PersonalDetailsBuilder();
    }

    public PersonalDetailsBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public PersonalDetailsBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public PersonalDetailsBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public PersonalDetailsBuilder withDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public PersonalDetailsBuilder withGender(Gender gender) {
        this.gender = gender;
        return this;
    }

    public PersonalDetailsBuilder withNationalInsuranceNumber(String nationalInsuranceNumber) {
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        return this;
    }

    public PersonalDetailsBuilder withDriverNumber(String driverNumber) {
        this.driverNumber = driverNumber;
        return this;
    }

    public PersonalDetailsBuilder withDriverLicenceDetails(String driverLicenceDetails) {
        this.driverLicenceDetails = driverLicenceDetails;
        return this;
    }

    public PersonalDetails build() {
        return new PersonalDetails(title, firstName, lastName, dateOfBirth, gender, nationalInsuranceNumber, driverNumber, driverLicenceDetails);

    }
}
