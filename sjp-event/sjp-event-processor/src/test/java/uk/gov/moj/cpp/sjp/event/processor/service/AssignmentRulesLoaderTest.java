package uk.gov.moj.cpp.sjp.event.processor.service;

import static junit.framework.TestCase.fail;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.AssignmentRuleType.ALLOW;
import static uk.gov.moj.cpp.sjp.domain.AssignmentRuleType.DISALLOW;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;

import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentRule;
import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentRules;
import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentRulesLoader;

import org.everit.json.schema.ValidationException;
import org.junit.Test;

public class AssignmentRulesLoaderTest {

    @Test
    public void shouldLoadDefaultConfiguration() throws Exception {
        final AssignmentRules prosecutorsAssignmentRules = new AssignmentRulesLoader().load();
        assertThat(prosecutorsAssignmentRules, not(nullValue()));
    }

    @Test
    public void shouldLoadCorrectConfiguration() throws Exception {
        final AssignmentRules assignmentsRules = new AssignmentRulesLoader().load("assignment_rules_correct.json");

        final AssignmentRule assignmentRule1 = new AssignmentRule("B01", asList(TFL.name()), ALLOW);
        final AssignmentRule assignmentRule2 = new AssignmentRule(EMPTY, asList(TFL.name()), DISALLOW);

        assertThat(assignmentsRules.getAssignmentRules(), containsInAnyOrder(assignmentRule1, assignmentRule2));
    }

    @Test
    public void shouldThrowExceptionWhenConfigurationDoesNotMatchSchema() throws Exception {
        try {
            new AssignmentRulesLoader().load("assignment_rules_incorrect.json");
            fail("Validation exception expected");
        } catch (final ValidationException exception) {
            assertThat(exception.getViolationCount(), is(2));
        }
    }

}
