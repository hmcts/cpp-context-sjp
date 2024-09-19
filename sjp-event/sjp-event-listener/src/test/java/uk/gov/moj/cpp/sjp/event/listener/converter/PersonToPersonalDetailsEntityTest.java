package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PersonToPersonalDetailsEntityTest {

    @Mock
    private AddressToAddressEntity addressToAddressEntityConverter;

    @Mock
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntityConverter;

    @InjectMocks
    private PersonToPersonalDetailsEntity<Person> converterUnderTest = new PersonToPersonalDetailsEntity<>();

    @Test
    public void shouldConvertPersonToPersonalDetailsEntity() {
        Person inputPerson = CaseBuilder.aDefaultSjpCase().build().getDefendant();
        final PersonalDetails outputPersonalDetails = converterUnderTest.convert(inputPerson);

        final PersonalDetails expectedPerson = new PersonalDetails(
                inputPerson.getTitle(),
                inputPerson.getFirstName(),
                inputPerson.getLastName(),
                inputPerson.getDateOfBirth(),
                inputPerson.getGender(),
                inputPerson.getNationalInsuranceNumber(),
                inputPerson.getDriverNumber(),
                inputPerson.getDriverLicenceDetails());

        assertTrue(reflectionEquals(outputPersonalDetails, expectedPerson));
    }

}
