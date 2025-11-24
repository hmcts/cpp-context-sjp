package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-details-moved-from-people")
public class DefendantDetailsMovedFromPeople {

    private final UUID caseId;
    private final UUID defendantId;
    private final UUID personId;
    private final String title;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final String nationalInsuranceNumber;
    private final Address address;
    private final ContactDetails contactNumber;

    //Created builder
    @JsonCreator
    private DefendantDetailsMovedFromPeople(@JsonProperty("caseId") UUID caseId,
                                            @JsonProperty("defendantId") UUID defendantId,
                                            @JsonProperty("personId") UUID personId,
                                            @JsonProperty("title") String title,
                                            @JsonProperty("firstName") String firstName,
                                            @JsonProperty("lastName") String lastName,
                                            @JsonProperty("dateOfBirth") LocalDate dateOfBirth,
                                            @JsonProperty("gender") Gender gender,
                                            @JsonProperty("nationalInsuranceNumber") String nationalInsuranceNumber,
                                            @JsonProperty("contactNumber") ContactDetails contactNumber,
                                            @JsonProperty("address") Address address) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.personId = personId;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.contactNumber = contactNumber;
        this.address = address;
    }

    public static class DefendantDetailsMovedFromPeopleBuilder {
        private UUID caseId;
        private UUID defendantId;
        private UUID personId;
        private String title;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private Gender gender;
        private String nationalInsuranceNumber;
        private Address address;
        private ContactDetails contactNumber;

        public static DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder defendantDetailsUpdated() {
            return new DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder();
        }

        public DefendantDetailsMovedFromPeopleBuilder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withPersonId(final UUID personId) {
            this.personId = personId;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withGender(final Gender gender) {
            this.gender = gender;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withAddress(final Address address) {
            this.address = address;
            return this;
        }

        public DefendantDetailsMovedFromPeopleBuilder withContactNumber(final ContactDetails contactNumber) {
            this.contactNumber = contactNumber;
            return this;
        }

        public DefendantDetailsMovedFromPeople build() {
            return new DefendantDetailsMovedFromPeople(caseId, defendantId, personId, title, firstName,
                    lastName, dateOfBirth, gender, nationalInsuranceNumber, contactNumber,
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

    public Gender getGender() {
        return gender;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public Address getAddress() {
        return address;
    }

    public ContactDetails getContactNumber() {
        return contactNumber;
    }
}
