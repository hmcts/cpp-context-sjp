package uk.gov.moj.cpp.sjp.persistence.entity;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class OnlinePleaPersonalDetails {
    @Column(name = "personal_details_first_name")
    private String firstName;
    @Column(name = "personal_details_last_name")
    private String lastName;

    @AttributeOverrides({
            @AttributeOverride(name="address1", column=@Column(name="personal_details_address1")),
            @AttributeOverride(name="address2", column=@Column(name="personal_details_address2")),
            @AttributeOverride(name="address3", column=@Column(name="personal_details_address3")),
            @AttributeOverride(name="address4", column=@Column(name="personal_details_address4")),
            @AttributeOverride(name="postcode", column=@Column(name="personal_details_postcode"))
    })
    private Address address;

    @Column(name = "personal_details_telephone_number_home")
    private String homeTelephone;
    @Column(name = "personal_details_telephone_number_mobile")
    private String mobile;
    @Column(name = "personal_details_email")
    private String email;
    @Column(name = "personal_details_date_of_birth")
    private String dateOfBirth;
    @Column(name = "personal_details_national_insurance_number")
    private String nationalInsuranceNumber;

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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getHomeTelephone() {
        return homeTelephone;
    }

    public void setHomeTelephone(String homeTelephone) {
        this.homeTelephone = homeTelephone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public void setNationalInsuranceNumber(String nationalInsuranceNumber) {
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

}
