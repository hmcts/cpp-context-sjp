package uk.gov.moj.cpp.sjp.event;

import static java.util.Optional.ofNullable;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties("personId")
@Event("sjp.events.defendant-details-updated")
public class DefendantDetailsUpdated {

    private final UUID caseId;
    private final UUID defendantId;
    private final String title;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final String nationalInsuranceNumber;
    private final String driverNumber;
    private final String driverLicenceDetails;
    private final Address address;
    private final ContactDetails contactDetails;
    private final boolean updateByOnlinePlea;
    private final ZonedDateTime updatedDate;
    private final String region;

    @JsonCreator
    private DefendantDetailsUpdated(@JsonProperty("caseId") UUID caseId, @JsonProperty("defendantId") UUID defendantId,
                                    @JsonProperty("title") String title,
                                    @JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName,
                                    @JsonProperty("dateOfBirth") LocalDate dateOfBirth, @JsonProperty("gender") Gender gender,
                                    @JsonProperty("nationalInsuranceNumber") String nationalInsuranceNumber,
                                    @JsonProperty("driverNumber") String driverNumber,
                                    @JsonProperty("driverLicenceDetails") String driverLicenceDetails,
                                    @JsonProperty("contactDetails") ContactDetails contactDetails, @JsonProperty("address") Address address,
                                    @JsonProperty("updateByOnlinePlea") boolean updateByOnlinePlea,
                                    @JsonProperty("updatedDate") ZonedDateTime updatedDate,
                                    @JsonProperty("region") String region) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.driverNumber = driverNumber;
        this.driverLicenceDetails = driverLicenceDetails;
        this.contactDetails = contactDetails;
        this.address = address;
        this.updateByOnlinePlea = updateByOnlinePlea;
        this.updatedDate = updatedDate;
        this.region = region;
    }

    public static class DefendantDetailsUpdatedBuilder {
        private UUID caseId;
        private UUID defendantId;
        private String title;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private Gender gender;
        private String nationalInsuranceNumber;
        private String driverNumber;
        private String driverLicenceDetails;
        private Address address;
        private ContactDetails contactDetails;
        private boolean updateByOnlinePlea;
        private ZonedDateTime updatedDate;
        private boolean containsUpdate = false;
        private String region;

        public static DefendantDetailsUpdatedBuilder defendantDetailsUpdated() {
            return new DefendantDetailsUpdatedBuilder();
        }

        public DefendantDetailsUpdatedBuilder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withTitle(final String title) {
            this.title = title;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withFirstName(final String firstName) {
            this.firstName = firstName;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withLastName(final String lastName) {
            this.lastName = lastName;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withGender(final Gender gender) {
            this.gender = gender;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withDriverNumber(final String driverNumber) {
            this.driverNumber = driverNumber;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withDriverLicenceDetails(final String driverLicenceDetails) {
            this.driverLicenceDetails = driverLicenceDetails;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withAddress(final Address address) {
            this.address = address;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withContactDetails(final ContactDetails contactDetails) {
            this.contactDetails = contactDetails;
            this.containsUpdate = true;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withUpdateByOnlinePlea(final boolean updateByOnlinePlea) {
            this.updateByOnlinePlea = updateByOnlinePlea;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withUpdatedDate(final ZonedDateTime updatedDate) {
            this.updatedDate = updatedDate;
            return this;
        }

        public boolean containsUpdate() {
            return containsUpdate;
        }

        public DefendantDetailsUpdatedBuilder withRegion(final String region){
            this.region = region;
            return this;
        }

        public DefendantDetailsUpdated build() {
            return new DefendantDetailsUpdated(caseId, defendantId, title, firstName,
                    lastName, dateOfBirth, gender, nationalInsuranceNumber, driverNumber, driverLicenceDetails,
                    contactDetails, address, updateByOnlinePlea, updatedDate, region);
        }
    }

    public UUID getCaseId() {
        return caseId;
    }

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

    public Address getAddress() {
        return address;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public boolean isUpdateByOnlinePlea() {
        return updateByOnlinePlea;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }

    public String getRegion() {
        return region;
    }
}
