package uk.gov.moj.cpp.sjp.domain.testutils;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.testutils.OffenceBuilder.createDefaultOffences;

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

    public DefendantBuilder withOffences(List<Offence> offenses) {
        this.offences = offenses;
        return this;
    }

    public Defendant build() {
        return new Defendant(id, title, firstName,
                lastName, dateOfBirth, gender, nationalInsuranceNumber,
                driverNumber, driverLicenceDetails, address, contactDetails, numPreviousConvictions,
                offences, hearingLanguage, languageNeeds, region);
    }

}
