package uk.gov.moj.cpp.sjp.domain.testutils;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.testutils.OffenceBuilder.createDefaultOffences;
import static uk.gov.moj.cpp.sjp.domain.testutils.OffenceBuilder.createPressRestrictableOffence;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.Language;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;

public class DefendantBuilder {

    private UUID id = DefaultTestData.DEFENDANT_ID;
    private String title = "Mr";
    private String firstName = "John";
    private String lastName = "Smith";
    private LocalDate dateOfBirth = LocalDate.of(1980, 12, 3);
    private Gender gender = Gender.MALE;
    private String nationalInsuranceNumber = RandomStringUtils.random(10);
    private String driverNumber = "TESTY708166G99KZ";
    private String driverLicenceDetails = RandomStringUtils.random(50);
    private Address address = new Address("street", "suburb", "town", "county", "address5", "AA1 2BB");
    private ContactDetails contactDetails = new ContactDetails("020734777", "020734888", "020734999", "email1@bbb.ccc", "email2@bbb.ccc");
    private String region = "test region";
    private int numPreviousConvictions = 3;
    private List<Offence> offences = createDefaultOffences(randomUUID());
    private Language hearingLanguage = null;
    private String languageNeeds = RandomStringUtils.random(10);
    private String asn = "asn";
    private String pncIdentifier = "pncId";
    private String legalEntityName = "legalEntityName";
    private UUID pcqId = DefaultTestData.PCQ_ID;

    public DefendantBuilder addOffence(final UUID offenceId) {
        this.offences.add(createPressRestrictableOffence(offenceId, false));
        return this;
    }

    public DefendantBuilder withOffences(final List<Offence> offences) {
        this.offences = offences;
        return this;
    }

    public DefendantBuilder withOffences(final UUID... offenceIds) {
        this.offences = OffenceBuilder.createDefaultOffences(offenceIds);
        return this;
    }

    public DefendantBuilder withTitle(final String title) {
        this.title = title;
        return this;
    }

    public DefendantBuilder withFirstName(final String firstName) {
        this.firstName = firstName;
        return this;
    }

    public DefendantBuilder withLastName(final String lastName) {
        this.lastName = lastName;
        return this;
    }

    public DefendantBuilder withDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public DefendantBuilder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        return this;
    }

    public DefendantBuilder withDriverNumber(final String driverNumber) {
        this.driverNumber = driverNumber;
        return this;
    }

    public DefendantBuilder withAddress(final Address address) {
        this.address = address;
        return this;
    }

    public DefendantBuilder withGender(final Gender gender) {
        this.gender = gender;
        return this;
    }

    public DefendantBuilder withDriverLicenceDetails(final String driverLicenceDetails) {
        this.driverLicenceDetails = driverLicenceDetails;
        return this;
    }

    public DefendantBuilder withContactDetails(final ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
        return this;
    }

    public DefendantBuilder withNumberOfConvictions(final int numPreviousConvictions) {
        this.numPreviousConvictions = numPreviousConvictions;
        return this;
    }

    public DefendantBuilder withAsn(final String asn) {
        this.asn = asn;
        return this;
    }

    public DefendantBuilder withPncIdentifier(final String pncIdentifier) {
        this.pncIdentifier = pncIdentifier;
        return this;
    }

    public DefendantBuilder withLegalEntityName(final String legalEntityName) {
        this.legalEntityName = legalEntityName;
        return this;
    }

    public DefendantBuilder withPcqId(final UUID pcqId) {
        this.pcqId = pcqId;
        return this;
    }

    public Defendant build() {
        return new Defendant(
                id,
                title,
                firstName,
                lastName,
                dateOfBirth,
                gender,
                nationalInsuranceNumber,
                driverNumber,
                driverLicenceDetails,
                address,
                contactDetails,
                numPreviousConvictions,
                offences,
                hearingLanguage,
                languageNeeds,
                region,
                asn,
                pncIdentifier,
                legalEntityName,
                pcqId);
    }
}
