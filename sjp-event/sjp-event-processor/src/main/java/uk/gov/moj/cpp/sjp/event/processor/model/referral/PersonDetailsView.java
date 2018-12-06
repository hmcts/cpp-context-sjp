package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import java.time.LocalDate;

public class PersonDetailsView {

    private final String title;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final String gender;
    private final String interpreterLanguageNeeds;
    private final String nationalityId;
    private final String ethnicityId;
    private final String documentationLanguageNeeds;
    private final String nationalInsuranceNumber;
    private final String occupation;
    private final String occupationCode;
    private final String specificRequirements;
    private final AddressView address;
    private final ContactView contact;

    @SuppressWarnings("squid:S00107")
    public PersonDetailsView(final String title,
                             final String firstName,
                             final String lastName,
                             final LocalDate dateOfBirth,
                             final String gender,
                             final String interpreterLanguageNeeds,
                             final String nationalityId,
                             final String ethnicityId,
                             final String documentationLanguageNeeds,
                             final String nationalInsuranceNumber,
                             final String occupation,
                             final String occupationCode,
                             final String specificRequirements,
                             final AddressView address,
                             final ContactView contact) {

        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.interpreterLanguageNeeds = interpreterLanguageNeeds;
        this.nationalityId = nationalityId;
        this.ethnicityId = ethnicityId;
        this.documentationLanguageNeeds = documentationLanguageNeeds;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.occupation = occupation;
        this.occupationCode = occupationCode;
        this.specificRequirements = specificRequirements;
        this.address = address;
        this.contact = contact;
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

    public String getNationalityId() {
        return nationalityId;
    }

    public String getEthnicityId() {
        return ethnicityId;
    }

    public String getDocumentationLanguageNeeds() {
        return documentationLanguageNeeds;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public String getOccupation() {
        return occupation;
    }

    public String getGender() {
        return gender;
    }

    public String getInterpreterLanguageNeeds() {
        return interpreterLanguageNeeds;
    }

    public AddressView getAddress() {
        return address;
    }

    public ContactView getContact() {
        return contact;
    }


    public String getOccupationCode() {
        return occupationCode;
    }

    public String getSpecificRequirements() {
        return specificRequirements;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String gender;
        private String interpreterLanguageNeeds;
        private String nationalityId;
        private String ethnicityId;
        private String documentationLanguageNeeds;
        private String nationalInsuranceNumber;
        private String occupation;
        private String occupationCode;
        private String specificRequirements;
        private AddressView address;
        private ContactView contact;

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder withGender(final String gender) {
            this.gender = gender;
            return this;
        }

        public Builder withInterpreterLanguageNeeds(final String interpreterLanguageNeeds) {
            this.interpreterLanguageNeeds = interpreterLanguageNeeds;
            return this;
        }

        public Builder withNationalityId(final String nationalityId) {
            this.nationalityId = nationalityId;
            return this;
        }

        public Builder withEthnicityId(final String ethnicityId) {
            this.ethnicityId = ethnicityId;
            return this;
        }

        public Builder withDocumentationLanguageNeeds(final String documentationLanguageNeeds) {
            this.documentationLanguageNeeds = documentationLanguageNeeds;
            return this;
        }

        public Builder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            return this;
        }

        public Builder withOccupation(final String occupation) {
            this.occupation = occupation;
            return this;
        }

        public Builder withOccupationCode(final String occupationCode) {
            this.occupationCode = occupationCode;
            return this;
        }

        public Builder withSpecificRequirements(final String specificRequirements) {
            this.specificRequirements = specificRequirements;
            return this;
        }

        public Builder withAddress(final AddressView address) {
            this.address = address;
            return this;
        }

        public Builder withContact(final ContactView contact) {
            this.contact = contact;
            return this;
        }

        public PersonDetailsView build() {
            return new PersonDetailsView(
                    title,
                    firstName,
                    lastName,
                    dateOfBirth,
                    gender,
                    interpreterLanguageNeeds,
                    nationalityId,
                    ethnicityId,
                    documentationLanguageNeeds,
                    nationalInsuranceNumber,
                    occupation,
                    occupationCode,
                    specificRequirements,
                    address,
                    contact);
        }
    }
}
