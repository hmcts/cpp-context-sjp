package uk.gov.moj.cpp.sjp.event.processor.results.converter;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.FIRSTNAME;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AssociatedPersonDetailsConverterTest {

    @Mock
    private PersonDetailsConverter personDetailsConverter;

    @Mock
    Person person;

    @InjectMocks
    AssociatedPersonDetailsConverter associatedPersonDetailsConverter;

    @Test
    public void shouldConvertAssociatedPersonDetailsWithMandatoryField() {

        when(personDetailsConverter.getPersonDetails(any(), any())).thenReturn(person);
        when(person.getFirstName()).thenReturn(FIRSTNAME);
        final PersonalDetails personalDetails = PersonalDetails.personalDetails().build();
        List<AssociatedPerson> associatedPersonList = associatedPersonDetailsConverter.getAssociatedPersons(personalDetails);
        assertThat(associatedPersonList.size(), is(1));
        assertThat(associatedPersonList.get(0).getPerson().getFirstName(), is(FIRSTNAME));
    }

    @Test
    public void shouldConvertAssociatedPersonDetailsWithoutMandatoryField() {

        List<AssociatedPerson> associatedPersonList = associatedPersonDetailsConverter.getAssociatedPersons(null);
        assertThat(associatedPersonList.size(), is(0));
    }
}
