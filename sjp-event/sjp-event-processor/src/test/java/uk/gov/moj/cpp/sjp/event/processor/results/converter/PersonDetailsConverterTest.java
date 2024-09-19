package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ADDRESS_LINE_1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.CJS_CODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.DATE_OF_BIRTH;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.FIRSTNAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.HOME_PHONE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.LASTNAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.TITLE;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PersonDetailsConverterTest {

    @InjectMocks
    PersonDetailsConverter personDetailsConverter;


    @Mock
    private IndependentContactNumberConverter independentContactNumberConverter;

    @Mock
    private AddressConverter addressConverter;

    @Test
    public void shouldConvertPersonDetailsOnlyMandatoryValues() {

        final Address address = Address.address().withAddress1(ADDRESS_LINE_1).build();
        final PersonalDetails personalDetails = PersonalDetails.personalDetails()
                .withLastName(LASTNAME)
                .withGender(uk.gov.justice.json.schemas.domains.sjp.Gender.MALE)
                .build();


        final String countryCJSCode = CJS_CODE;


        final Person person = personDetailsConverter.getPersonDetails(personalDetails, countryCJSCode);

        assertThat(person.getLastName(), is(LASTNAME));
        assertThat(person.getGender(), is(Gender.MALE));

    }

    @Test
    public void shouldConvertPersonDetailsAllValues() {

        final ContactNumber contactNumber = ContactNumber.contactNumber()
                .withHome(HOME_PHONE)
                .build();

        final Address address = Address.address().withAddress1(ADDRESS_LINE_1).build();
        final PersonalDetails personalDetails = PersonalDetails.personalDetails()
                .withFirstName(FIRSTNAME)
                .withLastName(LASTNAME)
                .withGender(uk.gov.justice.json.schemas.domains.sjp.Gender.MALE)
                .withTitle(TITLE)
                .withDateOfBirth(DATE_OF_BIRTH)
                .build();

        when(independentContactNumberConverter.getContact(any())).thenReturn(contactNumber);
        when(addressConverter.getAddress(any())).thenReturn(address);


        final String countryCJSCode = CJS_CODE;


        final Person person = personDetailsConverter.getPersonDetails(personalDetails, countryCJSCode);

        assertThat(person.getFirstName(), is(FIRSTNAME));
        assertThat(person.getLastName(), is(LASTNAME));
        assertThat(person.getTitle(), is(TITLE));
        assertThat(person.getGender(), is(Gender.MALE));
        assertThat(person.getDateOfBirth(), is(DATE_OF_BIRTH.toString()));
        assertThat(person.getContact(), is(notNullValue()));
        assertThat(person.getContact().getHome(), is(HOME_PHONE));
        assertThat(person.getAddress(), is(notNullValue()));
        assertThat(person.getAddress().getAddress1(), is(ADDRESS_LINE_1));

    }

}
