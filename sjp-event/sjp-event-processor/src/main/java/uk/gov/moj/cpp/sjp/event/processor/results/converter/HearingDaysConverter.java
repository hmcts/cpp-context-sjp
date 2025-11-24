package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

public class HearingDaysConverter {

    public List<HearingDay> convert(final JsonObject sjpSessionPayload) {
        final ZonedDateTime startedAt = ZonedDateTimes.fromString(sjpSessionPayload.getString("startedAt", null));
        final List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDay.hearingDay()
                .withSittingDay(startedAt)
                .withListedDurationMinutes(360)
                .build());
        return hearingDays;
    }

}
