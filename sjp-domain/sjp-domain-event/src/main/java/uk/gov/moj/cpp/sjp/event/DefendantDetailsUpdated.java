package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
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
    private final String gender;
    private final String nationalInsuranceNumber;
    private final Address address;
    private final ContactDetails contactDetails;
    private final boolean updateByOnlinePlea;
    private final ZonedDateTime updatedDate;

    @SuppressWarnings("squid:S00107") //Created builder
    @JsonCreator
    private DefendantDetailsUpdated(@JsonProperty("caseId") UUID caseId, @JsonProperty("defendantId") UUID defendantId,
                                    @JsonProperty("title") String title,
                                    @JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName,
                                    @JsonProperty("dateOfBirth") LocalDate dateOfBirth, @JsonProperty("gender") String gender,
                                    @JsonProperty("nationalInsuranceNumber") String nationalInsuranceNumber,
                                    @JsonProperty("contactDetails") ContactDetails contactDetails, @JsonProperty("address") Address address,
                                    @JsonProperty("updateByOnlinePlea") boolean updateByOnlinePlea,
                                    @JsonProperty("updatedDate") ZonedDateTime updatedDate) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.contactDetails = contactDetails;
        this.address = address;
        this.updateByOnlinePlea = updateByOnlinePlea;
        this.updatedDate = updatedDate;
    }

    public static class DefendantDetailsUpdatedBuilder {
        private UUID caseId;
        private UUID defendantId;
        private String title;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String gender;
        private String nationalInsuranceNumber;
        private Address address;
        private ContactDetails contactDetails;
        private boolean updateByOnlinePlea;
        private ZonedDateTime updatedDate;

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
            return this;
        }

        public DefendantDetailsUpdatedBuilder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withGender(final String gender) {
            this.gender = gender;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withAddress(final Address address) {
            this.address = address;
            return this;
        }

        public DefendantDetailsUpdatedBuilder withContactDetails(final ContactDetails contactDetails) {
            this.contactDetails = contactDetails;
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

        public DefendantDetailsUpdated build() {
            return new DefendantDetailsUpdated(caseId, defendantId, title, firstName,
                    lastName, dateOfBirth, gender, nationalInsuranceNumber, contactDetails,
                    address, updateByOnlinePlea, updatedDate);
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

    public String getGender() {
        return gender;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
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
}
