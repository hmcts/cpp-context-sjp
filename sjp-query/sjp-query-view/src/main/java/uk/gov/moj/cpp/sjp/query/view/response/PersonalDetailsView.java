package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Objects.nonNull;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;

public class PersonalDetailsView {

    private String title;

    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String nationalInsuranceNumber;

    private PersonalAddressView address;

    private ContactDetailsView contactDetails;

    private Boolean dobChanged;

    private Boolean addressChanged;

    private Boolean nameChanged;

    public PersonalDetailsView(final PersonalDetails personalDetails) {
        if (personalDetails != null) {
            this.title = personalDetails.getTitle();
            this.firstName = personalDetails.getFirstName();
            this.lastName = personalDetails.getLastName();
            this.dateOfBirth = personalDetails.getDateOfBirth();
            this.gender = personalDetails.getGender();
            this.nationalInsuranceNumber = personalDetails.getNationalInsuranceNumber();
            this.address = new PersonalAddressView(personalDetails.getAddress());
            this.contactDetails = new ContactDetailsView(personalDetails.getContactDetails());
            this.addressChanged = nonNull(personalDetails.getAddressUpdatedAt());
            this.dobChanged = nonNull(personalDetails.getDateOfBirthUpdatedAt());
            this.nameChanged = nonNull(personalDetails.getNameUpdatedAt());
        }
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

    public PersonalAddressView getAddress() {
        return address;
    }

    public ContactDetailsView getContactDetails() {
        return contactDetails;
    }

    public Boolean isNameChanged() {
        return nameChanged;
    }

    public Boolean isAddressChanged() {
        return addressChanged;
    }

    public Boolean isDobChanged() {
        return dobChanged;
    }

}