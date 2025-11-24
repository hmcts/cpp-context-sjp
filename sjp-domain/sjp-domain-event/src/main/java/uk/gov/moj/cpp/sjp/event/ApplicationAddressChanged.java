package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(ApplicationAddressChanged.EVENT_NAME)
public class ApplicationAddressChanged {

    public static final String EVENT_NAME = "sjp.events.application-address-changed";

    private UUID caseId;
    private UUID defendantId;
    private String title;
    private String firstName;
    private String lastName;
    private String driverNumber;
    private String driverLicenceDetails;
    private Gender gender;
    private String nationalInsuranceNumber;
    private LocalDate dateOfBirth;
    private String email;
    private String email2;
    private String legalEntityName;
    private ContactDetails contactNumber;
    private Address address;
    private String region;
    private String addressUpdateFromApplication;

    @JsonCreator
    private ApplicationAddressChanged(@JsonProperty("caseId") final UUID caseId,
                                      @JsonProperty("defendantId") final UUID defendantId,
                                      @JsonProperty("title") final String title,
                                      @JsonProperty("firstName") final String firstName,
                                      @JsonProperty("lastName") final String lastName,
                                      @JsonProperty("driverNumber") final String driverNumber,
                                      @JsonProperty("driverLicenceDetails") final String driverLicenceDetails,
                                      @JsonProperty("gender") final Gender gender,
                                      @JsonProperty("nationalInsuranceNumber") final String nationalInsuranceNumber,
                                      @JsonProperty("dateOfBirth") final LocalDate dateOfBirth,
                                      @JsonProperty("email") final String email,
                                      @JsonProperty("email2") final String email2,
                                      @JsonProperty("legalEntityName") final String legalEntityName,
                                      @JsonProperty("contactNumber") final ContactDetails contactNumber,
                                      @JsonProperty("address") final Address address,
                                      @JsonProperty("region") final String region,
                                      @JsonProperty("addressUpdateFromApplication") final String addressUpdateFromApplication) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.driverNumber = driverNumber;
        this.driverLicenceDetails = driverLicenceDetails;
        this.gender = gender;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.email2 = email2;
        this.legalEntityName = legalEntityName;
        this.contactNumber = contactNumber;
        this.address = address;
        this.region = region;
        this.addressUpdateFromApplication = addressUpdateFromApplication;
    }

    public UUID getCaseId() {return caseId;}
    public Address getAddress() {return address;}

    public UUID getDefendantId() {
        return defendantId;
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

    public String getDriverNumber() {
        return driverNumber;
    }

    public String getDriverLicenceDetails() {
        return driverLicenceDetails;
    }

    public Gender getGender() {
        return gender;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public String getEmail2() {
        return email2;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public ContactDetails getContactNumber() {
        return contactNumber;
    }

    public String getRegion() {
        return region;
    }

    public String getAddressUpdateFromApplication() {return addressUpdateFromApplication;}

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DefendantAddressChanged{");
        sb.append("caseId=").append(caseId);
        sb.append(", defendantId=").append(defendantId);
        sb.append("title=").append(title);
        sb.append(", firstName=").append(firstName);
        sb.append("lastName=").append(lastName);
        sb.append(", driverNumber=").append(driverNumber);
        sb.append("driverLicenceDetails=").append(driverLicenceDetails);
        sb.append(", gender=").append(gender);
        sb.append("nationalInsuranceNumber=").append(nationalInsuranceNumber);
        sb.append(", dateOfBirth=").append(dateOfBirth);
        sb.append("email=").append(email);
        sb.append(", email2=").append(email2);
        sb.append("legalEntityName=").append(legalEntityName);
        sb.append(", contactNumber=").append(contactNumber);
        sb.append(", address=").append(address);
        sb.append(", region=").append(region);
        sb.append(", addressUpdateFromApplication=").append(addressUpdateFromApplication);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final uk.gov.moj.cpp.sjp.event.ApplicationAddressChanged that = (uk.gov.moj.cpp.sjp.event.ApplicationAddressChanged) obj;

        return java.util.Objects.equals(this.caseId, that.caseId) &&
                java.util.Objects.equals(this.address, that.address) &&
                java.util.Objects.equals(this.addressUpdateFromApplication, that.addressUpdateFromApplication) ;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(caseId,address,addressUpdateFromApplication);
    }


    public static ApplicationAddressChanged.Builder applicationAddressChanged() {
        return new ApplicationAddressChanged.Builder();
    }

    public static class Builder {
        private UUID caseId;
        private UUID defendantId;
        private String title;
        private String firstName;
        private String lastName;
        private String driverNumber;
        private String driverLicenceDetails;
        private Gender gender;
        private String nationalInsuranceNumber;
        private LocalDate dateOfBirth;
        private String email;
        private String email2;
        private String legalEntityName;
        private ContactDetails contactNumber;
        private Address address;
        private String region;
        private String addressUpdateFromApplication;

        public ApplicationAddressChanged.Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public ApplicationAddressChanged.Builder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public ApplicationAddressChanged.Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public ApplicationAddressChanged.Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public ApplicationAddressChanged.Builder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public ApplicationAddressChanged.Builder withDriverNumber(final String driverNumber) {
            this.driverNumber = driverNumber;
            return this;
        }

        public ApplicationAddressChanged.Builder withDriverLicenceDetails(final String driverLicenceDetails) {
            this.driverLicenceDetails = driverLicenceDetails;
            return this;
        }

        public ApplicationAddressChanged.Builder withGender(final Gender gender) {
            this.gender = gender;
            return this;
        }

        public ApplicationAddressChanged.Builder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            return this;
        }

        public ApplicationAddressChanged.Builder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public ApplicationAddressChanged.Builder withEmail(final String email) {
            this.email = email;
            return this;
        }

        public ApplicationAddressChanged.Builder withEmail2(final String email2) {
            this.email2 = email2;
            return this;
        }

        public ApplicationAddressChanged.Builder withLegalEntityName(final String legalEntityName) {
            this.legalEntityName = legalEntityName;
            return this;
        }

        public ApplicationAddressChanged.Builder withContactNumber(final ContactDetails contactNumber) {
            this.contactNumber = contactNumber;
            return this;
        }

        public ApplicationAddressChanged.Builder withAddress(final Address address) {
            this.address = address;
            return this;
        }

        public ApplicationAddressChanged.Builder withRegion(final String region) {
            this.region = region;
            return this;
        }

        public ApplicationAddressChanged.Builder withAddressUpdateFromApplication(final String addressUpdateFromApplication) {
            this.addressUpdateFromApplication = addressUpdateFromApplication;
            return this;
        }

        public ApplicationAddressChanged build() {
            return new uk.gov.moj.cpp.sjp.event.ApplicationAddressChanged(caseId,defendantId,title,firstName,lastName,driverNumber,driverLicenceDetails,gender,nationalInsuranceNumber
                    ,dateOfBirth,email,email2,legalEntityName,contactNumber,address,region,addressUpdateFromApplication);
        }
    }
}