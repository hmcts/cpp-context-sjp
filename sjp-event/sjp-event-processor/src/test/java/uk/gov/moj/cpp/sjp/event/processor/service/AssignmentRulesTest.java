package uk.gov.moj.cpp.sjp.event.processor.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.AssignmentRuleType.ALLOW;
import static uk.gov.moj.cpp.sjp.domain.AssignmentRuleType.DISALLOW;

import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentRule;
import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentRules;

import org.junit.Test;

public class AssignmentRulesTest {

    @Test
    public void shouldReturnBestAssignmentRuleForOuCode() {
        final AssignmentRule assignmentRule1 = new AssignmentRule("B01", asList("TFL"), ALLOW);
        final AssignmentRule assignmentRule2 = new AssignmentRule("B01LY", asList("TFL"), DISALLOW);
        final AssignmentRule assignmentRule3 = new AssignmentRule("B23", asList("DVL"), ALLOW);
        final AssignmentRule assignmentRule4 = new AssignmentRule("", asList("DVL", "TFL"), DISALLOW);

        final AssignmentRules assignmentRules = new AssignmentRules(asList(assignmentRule1, assignmentRule2, assignmentRule3, assignmentRule4));

        assertThat(assignmentRules.getBestCaseAssignmentRule("X"), is(assignmentRule4));
        assertThat(assignmentRules.getBestCaseAssignmentRule("B"), is(assignmentRule4));
        assertThat(assignmentRules.getBestCaseAssignmentRule("B23"), is(assignmentRule3));
        assertThat(assignmentRules.getBestCaseAssignmentRule("B23HS"), is(assignmentRule3));
        assertThat(assignmentRules.getBestCaseAssignmentRule("B0"), is(assignmentRule4));
        assertThat(assignmentRules.getBestCaseAssignmentRule("B01"), is(assignmentRule1));
        assertThat(assignmentRules.getBestCaseAssignmentRule("B01L"), is(assignmentRule1));
        assertThat(assignmentRules.getBestCaseAssignmentRule("B01LY"), is(assignmentRule2));
    }

    @Test
    public void shouldReturnNonRestrictiveRuleWhenThereAreNoRulesDefined() {
        final AssignmentRules assignmentRules = new AssignmentRules(emptyList());

        assertThat(assignmentRules.getBestCaseAssignmentRule("B01"), is(new AssignmentRule(EMPTY, emptyList(), DISALLOW)));
    }

    @Test
    public void shouldReturnNonRestrictiveRuleWhenNoRulesMatch() {
        final AssignmentRule assignmentRule = new AssignmentRule("B01", asList("TFL"), ALLOW);
        final AssignmentRules assignmentRules = new AssignmentRules(asList(assignmentRule));

        assertThat(assignmentRules.getBestCaseAssignmentRule("B23"), is(new AssignmentRule(EMPTY, emptyList(), DISALLOW)));
    }
}
