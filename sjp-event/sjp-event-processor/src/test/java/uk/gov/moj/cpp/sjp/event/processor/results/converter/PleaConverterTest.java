package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.APPLICATION_ID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.DATE_TIME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.ID_2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.SESSION_ID;

import uk.gov.justice.json.schemas.domains.sjp.PleaMethod;
import uk.gov.justice.json.schemas.domains.sjp.PleaType;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.results.Plea;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaConverterTest {

    @InjectMocks
    PleaConverter pleaConverter;

    @Test
    public void shouldConvertPleaOnlyAllValues() {

        final Offence offence = Offence.offence()
                .withId(ID_2)
                .withPleaDate(DATE_TIME)
                .withPlea(PleaType.GUILTY)
                .build();


        final uk.gov.justice.core.courts.Plea actualPlea = pleaConverter.getPlea(offence, SESSION_ID);

        assertThat(actualPlea.getOffenceId(), is(ID_2));
        assertThat(actualPlea.getPleaDate(), is(DATE_TIME.format(DATE_FORMAT)));
        assertThat(actualPlea.getPleaValue(), is(PleaType.GUILTY.toString()));

    }

    @Test
    public void shouldConvertApplicationPleaOnlyMandatoryValues() {

        final Plea plea = Plea.plea()
                .withPleaType(PleaType.GUILTY)
                .withPleaMethod(PleaMethod.ONLINE)
                .withPleaDate(DATE_TIME)
                .build();


        final uk.gov.justice.core.courts.Plea actualPlea = pleaConverter.getApplicationPlea(APPLICATION_ID, plea, SESSION_ID);

        assertThat(actualPlea.getApplicationId(), is(APPLICATION_ID));
        assertThat(actualPlea.getPleaDate(), is(DATE_TIME.toString()));
        assertThat(actualPlea.getPleaValue(), is(PleaType.GUILTY.toString()));

    }

    @Test
    public void shouldConvertApplicationPleaAllValues() {

        final Plea plea = Plea.plea()
                .withPleaType(PleaType.GUILTY)
                .withPleaMethod(PleaMethod.ONLINE)
                .withPleaDate(DATE_TIME)
                .build();


        final uk.gov.justice.core.courts.Plea actualPlea = pleaConverter.getApplicationPlea(APPLICATION_ID, plea, SESSION_ID);

        assertThat(actualPlea.getApplicationId(), is(APPLICATION_ID));
        assertThat(actualPlea.getOriginatingHearingId(), is(SESSION_ID));
        assertThat(actualPlea.getPleaDate(), is(DATE_TIME.toString()));
        assertThat(actualPlea.getPleaValue(), is(PleaType.GUILTY.toString()));

    }

}
