package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.justice.json.schemas.domains.sjp.Gender;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class PersonalDetails implements Serializable {

    private static final long serialVersionUID = -5855721631812305949L;

    @Column(name = "title")
    private String title;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "national_insurance_number")
    private String nationalInsuranceNumber;

    @Embedded
    private Address address;

    @Embedded
    private ContactDetails contactDetails;

    @Column(name = "address_changed")
    private Boolean addressChanged;

    @Column(name = "dob_changed")
    private Boolean dobChanged;

    @Column(name = "personal_name_changed")
    private Boolean nameChanged;


    public PersonalDetails() {
        this.address = new Address();
        this.contactDetails = new ContactDetails();
    }

    public PersonalDetails(final String title,
                           final String firstName,
                           final String lastName,
                           final LocalDate dateOfBirth,
                           final Gender gender,
                           final String nationalInsuranceNumber,
                           final Address address,
                           final ContactDetails contactDetails) {
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.address = address;
        this.contactDetails = contactDetails;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public void setNationalInsuranceNumber(String nationalInsuranceNumber) {
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
    }

    public Boolean getAddressChanged() {
        return addressChanged;
    }

    public void setAddressChanged(Boolean addressChanged) { this.addressChanged = addressChanged; }

    public Boolean getDobChanged() {
        return dobChanged;
    }

    public void setDobChanged(Boolean dobChanged) { this.dobChanged = dobChanged; }

    public Boolean getNameChanged() {
        return nameChanged;
    }

    public void setNameChanged(Boolean nameChanged) { this.nameChanged = nameChanged; }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersonalDetails)) {
            return false;
        }

        final PersonalDetails that = (PersonalDetails) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(dateOfBirth, that.dateOfBirth) &&
                gender == that.gender &&
                Objects.equals(nationalInsuranceNumber, that.nationalInsuranceNumber) &&
                Objects.equals(address, that.address) &&
                Objects.equals(contactDetails, that.contactDetails) &&
                Objects.equals(addressChanged, that.addressChanged) &&
                Objects.equals(dobChanged, that.dobChanged) &&
                Objects.equals(nameChanged, that.nameChanged);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, firstName, lastName, dateOfBirth, gender, nationalInsuranceNumber,
                address, contactDetails, addressChanged, dobChanged, nameChanged);
    }
}