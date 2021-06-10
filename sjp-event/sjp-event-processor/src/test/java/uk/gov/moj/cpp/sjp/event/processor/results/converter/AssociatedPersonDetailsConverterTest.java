package uk.gov.moj.cpp.sjp.event.processor.results.converter;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.FIRSTNAME;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssociatedPersonDetailsConverterTest {

    @Mock
    private PersonDetailsConverter personDetailsConverter;

    @Mock
    Person person;

    @InjectMocks
    AssociatedPersonDetailsConverter associatedPersonDetailsConverter;

    @Test
    public void shouldConvertAssociatedPersonDetailsWithMandatoryField() {

        when(personDetailsConverter.getPersonDetails(any(), anyString())).thenReturn(person);
        when(person.getFirstName()).thenReturn(FIRSTNAME);
        final PersonalDetails personalDetails = PersonalDetails.personalDetails().build();
        List<AssociatedPerson> associatedPersonList = associatedPersonDetailsConverter.getAssociatedPersons(personalDetails);
        assertThat(associatedPersonList.size(), is(1));
        assertThat(associatedPersonList.get(0).getPerson().getFirstName(), is(FIRSTNAME));
    }

    @Test
    public void shouldConvertAssociatedPersonDetailsWithoutMandatoryField() {

        when(personDetailsConverter.getPersonDetails(any(), anyString())).thenReturn(person);
        List<AssociatedPerson> associatedPersonList = associatedPersonDetailsConverter.getAssociatedPersons(null);
        assertThat(associatedPersonList.size(), is(0));
    }
}
