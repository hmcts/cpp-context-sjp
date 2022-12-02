package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.Objects.isNull;

import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;

import java.time.LocalDate;

import javax.inject.Inject;

public class PersonDetailsConverter {


    @Inject
    private IndependentContactNumberConverter independentContactNumberConverter;

    @Inject
    private AddressConverter addressConverter;

    public Person getPersonDetails(final PersonalDetails personalDetails, final String countryCJSCode) {
        if (isNull(personalDetails)) {
            return null;
        }
        final LocalDate birthDate = personalDetails.getDateOfBirth();
        return Person.person()
                .withNationalityCode(countryCJSCode)
                .withLastName(personalDetails.getLastName())//Mandatory
                .withGender(Gender.valueOf(personalDetails.getGender().name()))//Mandatory
                .withTitle(personalDetails.getTitle())
                .withFirstName(personalDetails.getFirstName())
                .withContact(independentContactNumberConverter.getContact(personalDetails.getContactDetails()))
                .withDateOfBirth(birthDate != null ? personalDetails.getDateOfBirth().toString() : null)
                .withAddress(addressConverter.getAddress(personalDetails.getAddress()))
                .build();
    }
}
