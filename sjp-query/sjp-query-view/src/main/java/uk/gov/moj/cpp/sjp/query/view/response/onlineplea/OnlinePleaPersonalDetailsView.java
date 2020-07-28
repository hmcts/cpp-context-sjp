package uk.gov.moj.cpp.sjp.query.view.response.onlineplea;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaPersonalDetails;

import java.time.LocalDate;

public class OnlinePleaPersonalDetailsView {

    private String firstName;

    private String lastName;

    private Address address;

    private String homeTelephone;

    private String mobile;

    private String email;

    private LocalDate dateOfBirth;

    private String nationalInsuranceNumber;

    private String driverNumber;

    private String driverLicenceDetails;

    public OnlinePleaPersonalDetailsView(final OnlinePleaPersonalDetails personalDetails) {
        this.firstName = personalDetails.getFirstName();
        this.lastName = personalDetails.getLastName();
        this.address = personalDetails.getAddress();
        this.homeTelephone = personalDetails.getHomeTelephone();
        this.mobile = personalDetails.getMobile();
        this.email = personalDetails.getEmail();
        this.dateOfBirth = personalDetails.getDateOfBirth();
        this.nationalInsuranceNumber = personalDetails.getNationalInsuranceNumber();
        this.driverNumber = personalDetails.getDriverNumber();
        this.driverLicenceDetails = personalDetails.getDriverLicenceDetails();
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

    public String getHomeTelephone() {
        return homeTelephone;
    }

    public String getMobile() {
        return mobile;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
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
}
