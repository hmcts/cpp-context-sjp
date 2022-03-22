package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules;

import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCase;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class CaseRuleUtils {

    public static final String CC_CLOSED_STATUS = "CLOSED";
    public static final String CC_INACTIVE_STATUS = "INACTIVE";
    private static final int CLOSED_STATUS_NUM_OF_DAYS_THRESHOLD = 28;

    private CaseRuleUtils() {
    }

    public static boolean lastHearingDateWithinRange(List<DefendantCase.Hearing> hearings) {
        return CaseRuleUtils.dateWithinRange(findLastHearingDate(hearings));
    }

    public static LocalDate findLastHearingDate(List<DefendantCase.Hearing> hearings) {
        final LocalDate lastHearingDate =
                hearings.stream().
                         flatMap(hearing -> hearing.getHearingDates().stream()).
                         map(LocalDate::parse).
                         max(Comparator.naturalOrder()).orElse(LocalDate.now());
        return lastHearingDate;
    }

    public static boolean dateWithinRange(LocalDate date) {
        final LocalDate today = LocalDate.now();
        final long elapsedDays = Duration.between(date.atStartOfDay(), today.atStartOfDay()).toDays();
        return elapsedDays <= CLOSED_STATUS_NUM_OF_DAYS_THRESHOLD;
    }
}
