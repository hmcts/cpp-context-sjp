package uk.gov.moj.cpp.sjp.persistence.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "employer")
public class Employer {

    @Id
    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "name")
    private String name;

    @Column(name = "employee_reference")
    private String employeeReference;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address_1")
    private String address1;

    @Column(name = "address_2")
    private String address2;

    @Column(name = "address_3")
    private String address3;

    @Column(name = "address_4")
    private String address4;

    @Column(name = "post_code")
    private String postCode;

    public Employer() {
        //required for hibernate
    }

    public Employer(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmployeeReference() {
        return employeeReference;
    }

    public void setEmployeeReference(String employeeReference) {
        this.employeeReference = employeeReference;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4(String address4) {
        this.address4 = address4;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }
}
