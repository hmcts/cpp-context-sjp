package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.BAIL_STATUS;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.CJS_CODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.FIRSTNAME;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantBaseDefendantConverterTest {

    @InjectMocks
    PersonDefendantConverter personDefendantConverter;

    @Mock
    PersonDetailsConverter personDetailsConverter;

    @Mock
    JCachedReferenceData jCachedReferenceData;

    @Mock
    BailStatus bailStatus;

    @Mock
    Person person;

    @Mock
    Metadata metadata;

    @Test
    public void shouldConvertPersonDefendant() {

        final Defendant defendant = Defendant.defendant()
                .withPersonalDetails(PersonalDetails.personalDetails()
                        .withFirstName("name")
                        .build())
                .build();
        when(jCachedReferenceData.getBailStatus(anyObject(), anyObject())).thenReturn(bailStatus);
        when(bailStatus.getCode()).thenReturn(BAIL_STATUS);
        when(personDetailsConverter.getPersonDetails(anyObject(), anyObject())).thenReturn(person);
        when(person.getFirstName()).thenReturn(FIRSTNAME);

        final PersonDefendant personDefendant = personDefendantConverter.getPersonDefendant(defendant, CJS_CODE, metadata);

        assertThat(personDefendant.getBailStatus(), is(notNullValue()));
        assertThat(personDefendant.getBailStatus().toString(), is(BAIL_STATUS));
        assertThat(personDefendant.getPersonDetails(), is(notNullValue()));
        assertThat(personDefendant.getPersonDetails().getFirstName(), is(FIRSTNAME));
    }

    @Test
    public void shouldReturnNullIfPersonDefendantIsNull() {
        final Defendant defendant = Defendant.defendant().build();
        final PersonDefendant personDefendant = personDefendantConverter.getPersonDefendant(defendant, CJS_CODE, metadata);
        assertThat(personDefendant, nullValue());
    }


}
