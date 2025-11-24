package uk.gov.moj.cpp.sjp.persistence.entity;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class OnlinePleaLegalEntityDetails {
    @Column(name = "legal_entity_name")
    private String legalEntityName;

    @Column(name = "position_of_representative")
    private String positionOfRepresentative;

    @AttributeOverrides({
            @AttributeOverride(name="tradingMoreThan12Months", column=@Column(name="trading_more_than_twelve_months")),
            @AttributeOverride(name="numberOfEmployees", column=@Column(name="number_of_employees")),
            @AttributeOverride(name="grossTurnover", column=@Column(name="gross_turnover_whole_pounds")),
            @AttributeOverride(name="netTurnover", column=@Column(name="net_turnover_whole_pounds"))
    })

    @Embedded
    private LegalEntityFinancialMeans legalEntityFinancialMeans;

    @Column(name = "legal_entity_details_home_telephone")
    private String homeTelephone;
    @Column(name = "legal_entity_details_mobile_telephone")
    private String mobile;
    @Column(name = "legal_entity_details_email")
    private String email;

    @AttributeOverrides({
            @AttributeOverride(name="address1", column=@Column(name="legal_entity_details_address1")),
            @AttributeOverride(name="address2", column=@Column(name="legal_entity_details_address2")),
            @AttributeOverride(name="address3", column=@Column(name="legal_entity_details_address3")),
            @AttributeOverride(name="address4", column=@Column(name="legal_entity_details_address4")),
            @AttributeOverride(name="address5", column=@Column(name="legal_entity_details_address5")),
            @AttributeOverride(name="postcode", column=@Column(name="legal_entity_details_postcode"))
    })
    @Embedded
    private Address address;

    public  OnlinePleaLegalEntityDetails(String legalEntityName, String positionOfRepresentative, LegalEntityFinancialMeans legalEntityFinancialMeans, Address address, String homeTelePhone, String mobile, String email) {
        this.legalEntityName = legalEntityName;
        this.positionOfRepresentative = positionOfRepresentative;
        this.legalEntityFinancialMeans = legalEntityFinancialMeans;
        this.address  = address;
        this.homeTelephone = homeTelePhone;
        this.mobile  = mobile;
        this.email = email;
    }

    public OnlinePleaLegalEntityDetails() {

    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public void setLegalEntityName(final String legalEntityName) {
        this.legalEntityName = legalEntityName;
    }

    public String getPositionOfRepresentative() {
        return positionOfRepresentative;
    }

    public void setPositionOfRepresentative(final String positionOfRepresentative) {
        this.positionOfRepresentative = positionOfRepresentative;
    }

    public LegalEntityFinancialMeans getLegalEntityFinancialMeans() {
        return legalEntityFinancialMeans;
    }

    public void setLegalEntityFinancialMeans(final LegalEntityFinancialMeans legalEntityFinancialMeans) {
        this.legalEntityFinancialMeans = legalEntityFinancialMeans;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public String getHomeTelephone() {
        return homeTelephone;
    }

    public void setHomeTelephone(final String homeTelephone) {
        this.homeTelephone = homeTelephone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(final String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }
}
