package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.time.ZonedDateTime;
import java.util.List;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingDaysConverterTest {

    @InjectMocks
    HearingDaysConverter hearingDaysConverter;

    @Mock
    JsonObject sjpSessionPayload;


    @Test
    public void shouldConvertHearingDays() {

        final String sittingDays = ZonedDateTime.now().toString();
        when(sjpSessionPayload.getString(anyString(), anyString())).thenReturn(sittingDays);


        final List<HearingDay> hearingDays = hearingDaysConverter.convert(sjpSessionPayload);

        assertThat(hearingDays.size(), is(1));
        assertThat(hearingDays.get(0).getSittingDay(), is(ZonedDateTimes.fromString(sittingDays)));

    }


}
