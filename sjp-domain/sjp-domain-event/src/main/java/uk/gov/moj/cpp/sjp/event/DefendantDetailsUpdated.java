package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactNumber;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("structure.events.defendant-details-updated")
public class DefendantDetailsUpdated implements Serializable {

    private final UUID caseId;
    private final UUID defendantId;
    private final UUID personId;
    private final String title;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final String gender;
    private final String email;
    private final String nationalInsuranceNumber;
    private final Address address;
    private final ContactNumber contactNumber;

    @SuppressWarnings("squid:S00107") //Created builder
    @JsonCreator
    private DefendantDetailsUpdated(@JsonProperty("caseId") UUID caseId, @JsonProperty("defendantId") UUID defendantId,
                                    @JsonProperty("personId") UUID personId, @JsonProperty("title") String title,
                                    @JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName,
                                    @JsonProperty("dateOfBirth") LocalDate dateOfBirth, @JsonProperty("gender") String gender,
                                    @JsonProperty("email") String email, @JsonProperty("nationalInsuranceNumber") String nationalInsuranceNumber,
                                    @JsonProperty("contactNumber") ContactNumber contactNumber, @JsonProperty("address") Address address) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.personId = personId;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.email = email;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.contactNumber = contactNumber;
        this.address = address;
    }

    public static class DefendantDetailsUpdatedBuilder {
        private UUID caseId;
        private UUID defendantId;
        private UUID personId;
        private String title;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String gender;
        private String email;
        private String nationalInsuranceNumber;
        private Address address;
        private ContactNumber contactNumber;

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

        public DefendantDetailsUpdatedBuilder withPersonId(final UUID personId) {
            this.personId = personId;
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

        public DefendantDetailsUpdatedBuilder withEmail(final String email) {
            this.email = email;
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

        public DefendantDetailsUpdatedBuilder withContactNumber(final ContactNumber contactNumber) {
            this.contactNumber = contactNumber;
            return this;
        }

        public DefendantDetailsUpdated build() {
            return new DefendantDetailsUpdated(caseId, defendantId, personId, title, firstName,
                    lastName, dateOfBirth, gender, email, nationalInsuranceNumber, contactNumber,
                    address);
        }
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getPersonId() {
        return personId;
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

    public String getEmail() {
        return email;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public Address getAddress() {
        return address;
    }

    public ContactNumber getContactNumber() {
        return contactNumber;
    }
}
