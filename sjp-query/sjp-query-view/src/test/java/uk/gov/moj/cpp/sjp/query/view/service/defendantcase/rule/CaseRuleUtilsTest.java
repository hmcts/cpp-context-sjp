package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleUtils.dateWithinRange;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleUtils.lastHearingDateWithinRange;

import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCase;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseRuleUtilsTest {

    @Test
    public void shouldDatesFallWithinRange() {
        LocalDate today = LocalDate.now();
        assertTrue(dateWithinRange(today.minusDays(0)));
        assertTrue(dateWithinRange(today.minusDays(5)));
        assertTrue(dateWithinRange(today.minusDays(12)));
        assertTrue(dateWithinRange(today.minusDays(22)));
        assertTrue(dateWithinRange(today.minusDays(27)));
        assertTrue(dateWithinRange(today.minusDays(28)));
    }

    @Test
    public void shouldDatesFallOutsideRange() {
        LocalDate today = LocalDate.now();
        assertFalse(dateWithinRange(today.minusDays(29)));
        assertFalse(dateWithinRange(today.minusDays(32)));
        assertFalse(dateWithinRange(today.minusMonths(2)));
    }

    @Test
    public void shouldLastHearingDateFallWithinRange() {
        // single hearing and single hearing date within 28 days
        List<DefendantCase.Hearing> hearings = new LinkedList<>();
        DefendantCase.Hearing hearing = new DefendantCase.Hearing();
        hearing.setHearingDates(createRandomHearingDates(1, 10));
        hearings.add(hearing);
        assertTrue(lastHearingDateWithinRange(hearings));

        // single hearing and multiple hearing dates (last one within 28 days)
        hearings.clear();
        hearing = new DefendantCase.Hearing();
        hearing.setHearingDates(createRandomHearingDates(4, 28));
        hearings.add(hearing);
        assertTrue(lastHearingDateWithinRange(hearings));

        // multi hearings and multiple hearing dates (last one within 28 days)
        hearings.clear();
        hearing = new DefendantCase.Hearing();
        hearing.setHearingDates(createRandomHearingDates(2, 7));
        hearings.add(hearing);
        hearing = new DefendantCase.Hearing();
        hearing.setHearingDates(createRandomHearingDates(2, 30));
        hearings.add(hearing);
        assertTrue(lastHearingDateWithinRange(hearings));
    }

    @Test
    public void shouldLastHearingDateFallBeyondRange() {
        // single hearing and single hearing date within 28 days
        List<DefendantCase.Hearing> hearings = new LinkedList<>();
        DefendantCase.Hearing hearing = new DefendantCase.Hearing();
        hearing.setHearingDates(createRandomHearingDates(1, 30));
        hearings.add(hearing);
        assertFalse(lastHearingDateWithinRange(hearings));

        // single hearing and multiple hearing dates (last one beyond 28 days)
        hearings.clear();
        hearing = new DefendantCase.Hearing();
        hearing.setHearingDates(createRandomHearingDates(4, 29));
        hearings.add(hearing);
        assertFalse(lastHearingDateWithinRange(hearings));

        // multi hearings and multiple hearing dates (last one beyond 28 days)
        hearings.clear();
        hearing = new DefendantCase.Hearing();
        hearing.setHearingDates(createRandomHearingDates(2, 32));
        hearings.add(hearing);
        hearing = new DefendantCase.Hearing();
        hearing.setHearingDates(createRandomHearingDates(2, 30));
        hearings.add(hearing);
        assertFalse(lastHearingDateWithinRange(hearings));
    }

    private List<String> createRandomHearingDates(int numberOfDates, int todayOffset) {
        LocalDate today = LocalDate.now();
        List<String> hearingDates = new LinkedList<>();
        for (int i = 0; i < numberOfDates; i++) {
            hearingDates.add(today.minusDays(todayOffset++).toString());
        }

        return hearingDates;
    }
}
