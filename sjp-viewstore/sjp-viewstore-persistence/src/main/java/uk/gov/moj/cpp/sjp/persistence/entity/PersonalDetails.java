package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.justice.json.schemas.domains.sjp.Gender;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
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

    @Column(name = "driver_number")
    private String driverNumber;

    @Column(name = "driver_licence_details")
    private String driverLicenceDetails;

    @Column(name = "dob_updated_at")
    private ZonedDateTime dateOfBirthUpdatedAt;

    public PersonalDetails() {
    }

    public PersonalDetails(final String title,
                           final String firstName,
                           final String lastName,
                           final LocalDate dateOfBirth,
                           final Gender gender,
                           final String nationalInsuranceNumber,
                           final String driverNumber,
                           final String driverLicenceDetails) {

        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.driverNumber = driverNumber;
        this.driverLicenceDetails = driverLicenceDetails;
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

    public String getDriverNumber() {
        return driverNumber;
    }

    public void setDriverNumber(final String driverNumber) {
        this.driverNumber = driverNumber;
    }

    public void setDriverLicenceDetails(final String driverLicenceDetails) {
        this.driverLicenceDetails = driverLicenceDetails;
    }

    public String getDriverLicenceDetails() {
        return driverLicenceDetails;
    }

    public ZonedDateTime getDateOfBirthUpdatedAt() {
        return dateOfBirthUpdatedAt;
    }

    public void markDateOfBirthUpdated(final ZonedDateTime updateDate) {
        this.dateOfBirthUpdatedAt = updateDate;
    }

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
                Objects.equals(driverNumber, that.driverNumber) &&
                Objects.equals(driverLicenceDetails, that.driverLicenceDetails) &&
                Objects.equals(dateOfBirthUpdatedAt, that.dateOfBirthUpdatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, firstName, lastName, dateOfBirth, gender, nationalInsuranceNumber,
                driverNumber, driverLicenceDetails, dateOfBirthUpdatedAt);
    }
}
