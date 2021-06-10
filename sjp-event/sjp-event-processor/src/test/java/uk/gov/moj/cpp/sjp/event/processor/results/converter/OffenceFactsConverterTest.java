package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.VEHICLE_MAKE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.VEHICLE_REGISTRATION_MARK;

import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OffenceFactsConverterTest {

    @InjectMocks
    OffenceFactsConverter offenceFactsConverter;

    @Test
    public void shouldConvertOffenceFacts() {
        Offence offence = Offence.offence()
                .withVehicleMake(VEHICLE_MAKE)
                .withVehicleRegistrationMark(VEHICLE_REGISTRATION_MARK)
                .build();
        final OffenceFacts offenceFacts = offenceFactsConverter.getOffenceFacts(offence);

        assertThat(offenceFacts.getVehicleMake(), is(VEHICLE_MAKE));
        assertThat(offenceFacts.getVehicleRegistration(), is(VEHICLE_REGISTRATION_MARK));
    }

}
