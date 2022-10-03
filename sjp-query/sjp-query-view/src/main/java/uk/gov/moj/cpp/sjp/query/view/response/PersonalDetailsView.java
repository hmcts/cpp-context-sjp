package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;

public class PersonalDetailsView {

    private String title;

    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String nationalInsuranceNumber;

    private String driverNumber;

    private String driverLicenceDetails;

    private AddressView address;

    private ContactDetailsView contactDetails;

    private Boolean dobChanged;

    private Boolean addressChanged;

    private Boolean nameChanged;

    public PersonalDetailsView(final DefendantDetail defendantDetail) {
        final PersonalDetails personalDetails = defendantDetail.getPersonalDetails();
        if (personalDetails != null) {
            this.title = personalDetails.getTitle();
            this.firstName = personalDetails.getFirstName();
            this.lastName = personalDetails.getLastName();
            this.dateOfBirth = personalDetails.getDateOfBirth();
            this.gender = personalDetails.getGender();
            this.nationalInsuranceNumber = personalDetails.getNationalInsuranceNumber();
            this.driverNumber = personalDetails.getDriverNumber();
            this.driverLicenceDetails = personalDetails.getDriverLicenceDetails();
            this.addressChanged = nonNull(defendantDetail.getAddressUpdatedAt());
            this.dobChanged = nonNull(personalDetails.getDateOfBirthUpdatedAt());
            this.nameChanged = nonNull(defendantDetail.getNameUpdatedAt());
            this.address = ofNullable(defendantDetail.getAddress()).map(AddressView::new).orElse(null);
            this.contactDetails = new ContactDetailsView(defendantDetail.getContactDetails());
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

    public String getDriverNumber() {
        return driverNumber;
    }

    public String getDriverLicenceDetails() {
        return driverLicenceDetails;
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

    public AddressView getAddress() {
        return address;
    }

    public ContactDetailsView getContactDetails() {
        return contactDetails;
    }
}