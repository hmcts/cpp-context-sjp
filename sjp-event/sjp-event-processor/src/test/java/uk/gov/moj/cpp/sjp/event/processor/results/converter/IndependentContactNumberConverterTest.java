package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.HOME_PHONE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.MOBILE_PHONE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.PRIMARY_EMAIL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.SECONDARY_EMAIL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WORK_PHONE;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndependentContactNumberConverterTest {

    @InjectMocks
    IndependentContactNumberConverter independentContactNumberConverter;

    @Mock
    ContactDetails contactDetails;


    @Test
    public void shouldConvertIndependentContactNumber() {

        when(contactDetails.getBusiness()).thenReturn(WORK_PHONE);
        when(contactDetails.getHome()).thenReturn(HOME_PHONE);
        when(contactDetails.getMobile()).thenReturn(MOBILE_PHONE);
        when(contactDetails.getEmail()).thenReturn(PRIMARY_EMAIL);
        when(contactDetails.getEmail2()).thenReturn(SECONDARY_EMAIL);

        ContactNumber contactNumber = independentContactNumberConverter.getContact(contactDetails);

        assertThat(contactNumber.getWork(), is(WORK_PHONE));
        assertThat(contactNumber.getHome(), is(HOME_PHONE));
        assertThat(contactNumber.getMobile(), is(MOBILE_PHONE));
        assertThat(contactNumber.getPrimaryEmail(), is(PRIMARY_EMAIL));
        assertThat(contactNumber.getSecondaryEmail(), is(SECONDARY_EMAIL));
    }


}
